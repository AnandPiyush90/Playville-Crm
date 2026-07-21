package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.birthday.*;
import com.playville.crm.entity.*;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BirthdayBookingService {

    private static final BigDecimal GST_RATE     = new BigDecimal("0.18");
    private static final int        SLOT_HOURS   = 2;
    private static final int        SLOT_MINUTES = 30;

    private final BirthdayBookingRepository bookingRepository;
    private final CustomerRepository        customerRepository;
    private final KidRepository             kidRepository;
    private final BranchRepository          branchRepository;
    private final StaffRepository           staffRepository;
    private final BirthdayQuoteService      quoteService;
    private final BirthdayQuoteLineRepository quoteLineRepository;
    private final BirthdayCatalogItemRepository birthdayCatalogItemRepository;
    private final BirthdayInventoryReservationService inventoryReservationService;

    @Transactional
    public BirthdayBookingResponse createBooking(BirthdayBookingRequest req,
                                                  String username) {
        Integer branchId = BranchContext.getBranchId();
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));

        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer", req.getCustomerId()));

        Kid kid = kidRepository.findById(req.getKidId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kid", req.getKidId()));

        if (!kid.getCustomer().getId().equals(customer.getId()))
            throw new BusinessRuleException(
                    "Kid does not belong to this customer");

        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username)
                .orElse(null);

        LocalTime slotEnd = req.getPartySlotStart()
                .plusHours(SLOT_HOURS).plusMinutes(SLOT_MINUTES);
        if (req.getQuote() != null) {
            return createFromQuote(req, branch, customer, kid, staff, slotEnd);
        }

        BigDecimal base     = req.getBaseAmount() != null
                ? req.getBaseAmount() : BigDecimal.ZERO;
        BigDecimal discPct  = req.getDiscountPct() != null
                ? req.getDiscountPct() : BigDecimal.ZERO;
        BigDecimal discAmt  = base.multiply(discPct)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal afterDisc = base.subtract(discAmt);
        BigDecimal gst      = afterDisc.multiply(GST_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total    = afterDisc.add(gst);
        BigDecimal advance  = req.getAdvancePaid() != null
                ? req.getAdvancePaid() : BigDecimal.ZERO;
        if (advance.compareTo(total) > 0)
            throw new BusinessRuleException("ADVANCE_EXCEEDS_TOTAL: Advance cannot exceed the birthday booking total");

        BirthdayBooking booking = BirthdayBooking.builder()
                .branch(branch)
                .customer(customer)
                .kid(kid)
                .staff(staff)
                .partyDate(req.getPartyDate())
                .partySlotStart(req.getPartySlotStart())
                .partySlotEnd(slotEnd)
                .expectedGuests(req.getExpectedGuests() != null
                        ? req.getExpectedGuests() : 10)
                .cakeOption(req.getCakeOption())
                .foodBoxesCount(req.getFoodBoxesCount() != null
                        ? req.getFoodBoxesCount() : 0)
                .baseAmount(base)
                .discountPct(discPct)
                .discountAmount(discAmt)
                .gstAmount(gst)
                .totalAmount(total)
                .advancePaid(advance)
                .paymentMode(req.getPaymentMode() != null
                        ? req.getPaymentMode() : Purchase.PaymentMode.Cash)
                .paymentReference(req.getPaymentReference())
                .notes(req.getNotes())
                .build();

        return toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BirthdayBookingResponse> getBranchBookings(
            LocalDate from, LocalDate to) {
        Integer branchId = BranchContext.getBranchId();
        if (from != null && to != null)
            return bookingRepository
                    .findByBranchIdAndPartyDateBetweenOrderByPartyDateAsc(
                            branchId, from, to)
                    .stream().map(this::toResponse).toList();
        return bookingRepository
                .findByBranchIdOrderByPartyDateAsc(branchId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BirthdayBookingResponse getBookingById(Integer id) {
        BirthdayBooking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BirthdayBooking", id));
        if (!b.getBranch().getId().equals(BranchContext.getBranchId()))
            throw new BranchAccessDeniedException();
        return toResponse(b);
    }

    @Transactional
    public BirthdayBookingResponse updateStatus(Integer id,
                                                 BirthdayBooking.BookingStatus status) {
        BirthdayBooking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BirthdayBooking", id));
        if (!b.getBranch().getId().equals(BranchContext.getBranchId()))
            throw new BranchAccessDeniedException();
        if (!isAllowedStatusTransition(b.getStatus(), status))
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION: Use the permitted birthday booking workflow actions");
        if (status == BirthdayBooking.BookingStatus.Confirmed && b.getStatus() != BirthdayBooking.BookingStatus.Confirmed) {
            requireAvailable(b.getBranch().getId(), b.getPartyDate(), b.getPartySlotStart(), b.getPartySlotEnd(), b.getId());
            inventoryReservationService.reserve(b);
        }
        if (status == BirthdayBooking.BookingStatus.Cancelled && b.getStatus() != BirthdayBooking.BookingStatus.Cancelled) inventoryReservationService.release(b);
        b.setStatus(status);
        return toResponse(bookingRepository.save(b));
    }

    @Transactional
    public BirthdayBookingResponse complete(Integer id, BirthdayCompletionRequest request) {
        BirthdayBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BirthdayBooking", id));
        if (!booking.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
        if (booking.getStatus() == BirthdayBooking.BookingStatus.Cancelled) {
            throw new BusinessRuleException("CANCELLED_BOOKING: A cancelled birthday booking cannot be completed");
        }
        booking.setActualKids(request.getActualKids());
        booking.setActualAdults(request.getActualAdults());
        booking.setActualExtraMinutes(request.getActualExtraMinutes() == null ? 0 : request.getActualExtraMinutes());
        booking.setCompletionNotes(request.getCompletionNotes());
        if (booking.getStatus() == BirthdayBooking.BookingStatus.Enquiry) inventoryReservationService.reserve(booking);
        inventoryReservationService.consume(booking);
        booking.setStatus(BirthdayBooking.BookingStatus.Completed);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BirthdayBookingResponse reschedule(Integer id, BirthdayRescheduleRequest request) {
        BirthdayBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BirthdayBooking", id));
        if (!booking.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
        if (booking.getStatus() == BirthdayBooking.BookingStatus.Completed || booking.getStatus() == BirthdayBooking.BookingStatus.Cancelled) {
            throw new BusinessRuleException("BOOKING_NOT_RESCHEDULABLE: Completed or cancelled birthday bookings cannot be rescheduled");
        }
        LocalTime slotEnd = request.getPartySlotStart().plusHours(SLOT_HOURS).plusMinutes(SLOT_MINUTES);
        if (booking.getStatus() == BirthdayBooking.BookingStatus.Confirmed) {
            requireAvailable(booking.getBranch().getId(), request.getPartyDate(), request.getPartySlotStart(), slotEnd, booking.getId());
        }
        booking.setPartyDate(request.getPartyDate());
        booking.setPartySlotStart(request.getPartySlotStart());
        booking.setPartySlotEnd(slotEnd);
        return toResponse(bookingRepository.save(booking));
    }

    private BirthdayBookingResponse toResponse(BirthdayBooking b) {
        BigDecimal balanceDue = b.getTotalAmount().subtract(b.getAdvancePaid());
        return BirthdayBookingResponse.builder()
                .id(b.getId())
                .branchId(b.getBranch().getId())
                .branchCode(b.getBranch().getBranchCode())
                .customerId(b.getCustomer().getId())
                .parentName(b.getCustomer().getParentName())
                .kidId(b.getKid().getId())
                .kidName(b.getKid().getKidName())
                .partyDate(b.getPartyDate())
                .partySlotStart(b.getPartySlotStart())
                .partySlotEnd(b.getPartySlotEnd())
                .expectedGuests(b.getExpectedGuests())
                .actualKids(b.getActualKids())
                .actualAdults(b.getActualAdults())
                .actualExtraMinutes(b.getActualExtraMinutes())
                .completionNotes(b.getCompletionNotes())
                .cakeOption(b.getCakeOption())
                .foodBoxesCount(b.getFoodBoxesCount())
                .baseAmount(b.getBaseAmount())
                .discountPct(b.getDiscountPct())
                .discountAmount(b.getDiscountAmount())
                .gstAmount(b.getGstAmount())
                .totalAmount(b.getTotalAmount())
                .advancePaid(b.getAdvancePaid())
                .balanceDue(balanceDue)
                .paymentMode(b.getPaymentMode())
                .paymentReference(b.getPaymentReference())
                .status(b.getStatus())
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .inventoryReservationStatus(inventoryReservationService.status(b.getId()))
                .build();
    }

    private BirthdayBookingResponse createFromQuote(BirthdayBookingRequest req, Branch branch,
                                                     Customer customer, Kid kid, Staff staff,
                                                     LocalTime slotEnd) {
        BirthdayQuotePreviewResponse quote = quoteService.preview(req.getQuote());
        BigDecimal advance = req.getAdvancePaid() == null ? BigDecimal.ZERO : req.getAdvancePaid();
        if (advance.compareTo(quote.getGrandTotal()) > 0)
            throw new BusinessRuleException("ADVANCE_EXCEEDS_TOTAL: Advance cannot exceed the birthday quote total");
        BirthdayBooking booking = BirthdayBooking.builder()
                .branch(branch).customer(customer).kid(kid).staff(staff)
                .partyDate(req.getPartyDate()).partySlotStart(req.getPartySlotStart()).partySlotEnd(slotEnd)
                .expectedGuests(req.getQuote().getKidsCount() + req.getQuote().getAdultsCount())
                .foodBoxesCount(req.getQuote().getKidsFoodBox() == null ? 0 : req.getQuote().getKidsFoodBox().getBoxCount())
                .baseAmount(quote.getSubtotal()).discountPct(BigDecimal.ZERO).discountAmount(BigDecimal.ZERO)
                .gstAmount(quote.getTaxTotal()).totalAmount(quote.getGrandTotal())
                .advancePaid(advance)
                .paymentMode(req.getPaymentMode() == null ? Purchase.PaymentMode.Cash : req.getPaymentMode())
                .paymentReference(req.getPaymentReference()).notes(req.getNotes()).build();
        BirthdayBooking saved = bookingRepository.save(booking);
        List<BirthdayQuoteLine> lines = new java.util.ArrayList<>();
        int lineNumber = 1;
        for (BirthdayQuotePreviewResponse.QuoteLine line : quote.getLines()) {
            BirthdayCatalogItem catalogItem = line.getCatalogItemId() == null ? null : birthdayCatalogItemRepository.findById(line.getCatalogItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("BirthdayCatalogItem", line.getCatalogItemId()));
            lines.add(BirthdayQuoteLine.builder().birthdayBooking(saved).lineNumber(lineNumber++)
                    .category(line.getCategory()).catalogItem(catalogItem).sku(catalogItem == null ? null : catalogItem.getSku())
                    .inventoryReservationRequired(line.isInventoryReservedOnConfirmation()).descriptionSnapshot(line.getDescription())
                    .quantity(line.getQuantity()).unitPrice(line.getUnitPrice())
                    .taxRate(line.getTaxRate()).lineTotal(line.getLineTotal()).build());
        }
        quoteLineRepository.saveAll(lines);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public boolean isSlotAvailable(LocalDate partyDate, LocalTime slotStart) {
        LocalTime slotEnd = slotStart.plusHours(SLOT_HOURS).plusMinutes(SLOT_MINUTES);
        return !hasActiveSlotConflict(BranchContext.getBranchId(), partyDate, slotStart, slotEnd, null);
    }

    private void requireAvailable(Integer branchId, LocalDate partyDate, LocalTime slotStart, LocalTime slotEnd, Integer excludedBookingId) {
        if (hasActiveSlotConflict(branchId, partyDate, slotStart, slotEnd, excludedBookingId)) {
            throw new DuplicateResourceException("SLOT_CONFLICT: This time overlaps an active birthday booking");
        }
    }

    private boolean hasActiveSlotConflict(Integer branchId, LocalDate partyDate, LocalTime slotStart, LocalTime slotEnd, Integer excludedBookingId) {
        return bookingRepository
                .findByBranchIdAndPartyDateAndPartySlotStartLessThanAndPartySlotEndGreaterThanAndStatusNot(
                        branchId, partyDate, slotEnd, slotStart, BirthdayBooking.BookingStatus.Cancelled)
                .stream()
                .anyMatch(other -> !Objects.equals(other.getId(), excludedBookingId)
                        && other.getStatus() == BirthdayBooking.BookingStatus.Confirmed);
    }

    private boolean isAllowedStatusTransition(BirthdayBooking.BookingStatus current, BirthdayBooking.BookingStatus requested) {
        if (current == requested) return true;
        return switch (current) {
            case Enquiry -> requested == BirthdayBooking.BookingStatus.Confirmed || requested == BirthdayBooking.BookingStatus.Cancelled;
            case Confirmed -> requested == BirthdayBooking.BookingStatus.Cancelled;
            case Completed, Cancelled -> false;
        };
    }
}
