package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.schooltrip.*;
import com.playville.crm.entity.*;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchoolTripService {

    private static final BigDecimal GST_RATE = new BigDecimal("0.18");

    private final SchoolTripRepository schoolTripRepository;
    private final BranchRepository     branchRepository;
    private final StaffRepository      staffRepository;

    @Transactional
    public SchoolTripResponse createTrip(SchoolTripRequest req, String username) {

        Integer branchId = BranchContext.getBranchId();
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));

        // Slot conflict guard
        schoolTripRepository.findByBranchIdAndTripDateAndSlotStart(
                branchId, req.getTripDate(), req.getSlotStart())
                .ifPresent(t -> { throw new DuplicateResourceException(
                        "Slot already booked at "
                        + req.getSlotStart()
                        + " on " + req.getTripDate()); });

        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username)
                .orElse(null);

        BigDecimal total    = req.getTotalAmount() != null
                ? req.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal gst      = total.multiply(GST_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal advance  = req.getAdvancePaid() != null
                ? req.getAdvancePaid() : BigDecimal.ZERO;

        SchoolTrip trip = SchoolTrip.builder()
                .branch(branch)
                .staff(staff)
                .schoolName(req.getSchoolName())
                .contactPerson(req.getContactPerson())
                .contactPhone(req.getContactPhone())
                .contactEmail(req.getContactEmail())
                .tripDate(req.getTripDate())
                .slotStart(req.getSlotStart())
                .slotEnd(req.getSlotEnd())
                .expectedKids(req.getExpectedKids() != null
                        ? req.getExpectedKids() : 0)
                .pricePerKid(req.getPricePerKid() != null
                        ? req.getPricePerKid() : BigDecimal.ZERO)
                .totalAmount(total)
                .gstAmount(gst)
                .advancePaid(advance)
                .paymentMode(req.getPaymentMode() != null
                        ? req.getPaymentMode() : Purchase.PaymentMode.Online)
                .paymentReference(req.getPaymentReference())
                .notes(req.getNotes())
                .build();

        return toResponse(schoolTripRepository.save(trip));
    }

    @Transactional(readOnly = true)
    public List<SchoolTripResponse> getBranchTrips(LocalDate from, LocalDate to) {
        Integer branchId = BranchContext.getBranchId();
        if (from != null && to != null)
            return schoolTripRepository
                    .findByBranchIdAndTripDateBetweenOrderByTripDateAsc(
                            branchId, from, to)
                    .stream().map(this::toResponse).toList();
        return schoolTripRepository
                .findByBranchIdOrderByTripDateAsc(branchId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SchoolTripResponse getTripById(Integer id) {
        SchoolTrip t = schoolTripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolTrip", id));
        if (!t.getBranch().getId().equals(BranchContext.getBranchId()))
            throw new BranchAccessDeniedException();
        return toResponse(t);
    }

    @Transactional
    public SchoolTripResponse updateStatus(Integer id,
                                            BirthdayBooking.BookingStatus status,
                                            Integer actualKids) {
        SchoolTrip t = schoolTripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolTrip", id));
        if (!t.getBranch().getId().equals(BranchContext.getBranchId()))
            throw new BranchAccessDeniedException();
        t.setStatus(status);
        if (actualKids != null) t.setActualKids(actualKids);
        return toResponse(schoolTripRepository.save(t));
    }

    private SchoolTripResponse toResponse(SchoolTrip t) {
        BigDecimal balanceDue = t.getTotalAmount().subtract(t.getAdvancePaid());
        return SchoolTripResponse.builder()
                .id(t.getId())
                .branchId(t.getBranch().getId())
                .branchCode(t.getBranch().getBranchCode())
                .schoolName(t.getSchoolName())
                .contactPerson(t.getContactPerson())
                .contactPhone(t.getContactPhone())
                .contactEmail(t.getContactEmail())
                .tripDate(t.getTripDate())
                .slotStart(t.getSlotStart())
                .slotEnd(t.getSlotEnd())
                .expectedKids(t.getExpectedKids())
                .actualKids(t.getActualKids())
                .pricePerKid(t.getPricePerKid())
                .totalAmount(t.getTotalAmount())
                .gstAmount(t.getGstAmount())
                .advancePaid(t.getAdvancePaid())
                .balanceDue(balanceDue)
                .paymentMode(t.getPaymentMode())
                .paymentReference(t.getPaymentReference())
                .status(t.getStatus())
                .notes(t.getNotes())
                .createdAt(t.getCreatedAt())
                .build();
    }
}