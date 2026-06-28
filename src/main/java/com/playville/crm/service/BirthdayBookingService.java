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

        // Slot conflict guard
        bookingRepository.findByBranchIdAndPartyDateAndPartySlotStart(
                branchId, req.getPartyDate(), req.getPartySlotStart())
                .ifPresent(b -> { throw new DuplicateResourceException(
                        "Slot already booked at "
                        + req.getPartySlotStart()
                        + " on " + req.getPartyDate()); });

        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username)
                .orElse(null);

        LocalTime slotEnd = req.getPartySlotStart()
                .plusHours(SLOT_HOURS).plusMinutes(SLOT_MINUTES);

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
        b.setStatus(status);
        return toResponse(bookingRepository.save(b));
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
                .build();
    }
}