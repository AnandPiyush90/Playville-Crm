package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.checkin.*;
import com.playville.crm.dto.kid.KidDto;
import com.playville.crm.entity.*;
import com.playville.crm.entity.SessionDeduction.DeductionSource;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinService {

    private static final BigDecimal GST_RATE = new BigDecimal("0.18");

    private final CheckinRepository               checkinRepository;
    private final CheckinKidRepository            checkinKidRepository;
    private final CustomerRepository              customerRepository;
    private final KidRepository                   kidRepository;
    private final BranchRepository                branchRepository;
    private final StaffRepository                 staffRepository;
    private final SessionDeductionRepository      deductionRepository;
    private final InterBranchSettlementRepository settlementRepository;

    // ─── Check-in ────────────────────────────────────────────────

    @Transactional
    public CheckinResponse checkin(CheckinRequest req, String username) {

        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer", req.getCustomerId()));

        // Guard: already has active check-in
        checkinRepository.findByCustomerIdAndStatus(
                customer.getId(), Checkin.CheckinStatus.Active)
                .ifPresent(c -> { throw new BusinessRuleException(
                        "Customer already has an active check-in (ID: "
                        + c.getId() + ")"); });

        // Guard: sufficient balance
        if (customer.getGlobalSessionBalance() < req.getKidIds().size())
            throw new BusinessRuleException(
                    "Insufficient session balance. Available: "
                    + customer.getGlobalSessionBalance()
                    + ", Required: " + req.getKidIds().size());

        Integer branchId = BranchContext.getBranchId();
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));

        // Resolve staff from username — null-safe, staff column is nullable
        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username)
                .orElse(null);

        Checkin checkin = Checkin.builder()
                .branch(branch)
                .customer(customer)
                .staff(staff)
                .checkinTime(LocalDateTime.now())
                .status(Checkin.CheckinStatus.Active)
                .kidsCount(req.getKidIds().size())
                .build();

        Checkin saved = checkinRepository.save(checkin);

        List<KidDto> kidDtos = req.getKidIds().stream().map(kidId -> {
            Kid kid = kidRepository.findById(kidId)
                    .orElseThrow(() -> new ResourceNotFoundException("Kid", kidId));

            if (!kid.getCustomer().getId().equals(customer.getId()))
                throw new BusinessRuleException(
                        "Kid " + kidId + " does not belong to customer "
                        + customer.getId());

            if (!kid.isEligible())
                throw new BusinessRuleException(
                        kid.getKidName() + " is over 8 years old and not eligible");

            CheckinKid ck = CheckinKid.builder()
                    .checkin(saved)
                    .kid(kid)
                    .sessionUsed(false)
                    .build();
            checkinKidRepository.save(ck);

            return toKidDto(kid);
        }).toList();

        return CheckinResponse.builder()
                .checkinId(saved.getId())
                .customerId(customer.getId())
                .parentName(customer.getParentName())
                .phoneNumber(customer.getPhoneNumber())
                .branchId(branch.getId())
                .branchCode(branch.getBranchCode())
                .checkinTime(saved.getCheckinTime())
                .status(saved.getStatus().name())
                .kidsCount(saved.getKidsCount())
                .kids(kidDtos)
                .sessionBalance(customer.getGlobalSessionBalance())
                .build();
    }

    // ─── Checkout ────────────────────────────────────────────────

    @Transactional
    public CheckoutResponse checkout(Integer checkinId,
                                     CheckoutRequest req,
                                     String username) {

        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Checkin", checkinId));

        if (checkin.getStatus() != Checkin.CheckinStatus.Active)
            throw new BusinessRuleException(
                    "Check-in is already " + checkin.getStatus().name());

        if (!checkin.getBranch().getId().equals(BranchContext.getBranchId()))
            throw new BranchAccessDeniedException();

        Customer customer = checkin.getCustomer();
        Integer  branchId = BranchContext.getBranchId();
        Branch   branch   = checkin.getBranch();

        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username)
                .orElse(null);

        BigDecimal extraCharges = req.getExtraCharges() != null
                ? req.getExtraCharges() : BigDecimal.ZERO;
        BigDecimal gstAmount = extraCharges
                .multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCharged = extraCharges.add(gstAmount);

        List<CheckinKid> checkinKids = checkinKidRepository
                .findByCheckinIdAndSessionUsedFalse(checkinId);

        int balanceBefore    = customer.getGlobalSessionBalance();
        int sessionsDeducted = 0;
        boolean crossBranch  = false;

        for (CheckinKid ck : checkinKids) {
            int before = customer.getGlobalSessionBalance();
            customer.setGlobalSessionBalance(before - 1);
            int after = customer.getGlobalSessionBalance();

            ck.setSessionUsed(true);
            checkinKidRepository.save(ck);

            SessionDeduction deduction = SessionDeduction.builder()
                    .customer(customer)
                    .checkin(checkin)
                    .kid(ck.getKid())
                    .branch(branch)
                    .staff(staff)
                    .deductedAt(LocalDateTime.now())
                    .deductionSource(DeductionSource.Checkout)
                    .balanceBefore(before)
                    .balanceAfter(after)
                    .build();
            deductionRepository.save(deduction);
            sessionsDeducted++;

            // Inter-branch settlement
            if (customer.getPurchaseBranch() != null
                    && !customer.getPurchaseBranch().getId().equals(branchId)) {
                crossBranch = true;
                BigDecimal rate   = customer.getPurchaseBranch().getSettlementRate();
                BigDecimal amount = rate.divide(
                        new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                InterBranchSettlement settlement = InterBranchSettlement.builder()
                        .customer(customer)
                        .checkin(checkin)
                        .purchaseBranch(customer.getPurchaseBranch())
                        .usageBranch(branch)
                        .sessionsUsed(1)
                        .settlementRate(rate)
                        .settlementAmount(amount)
                        .build();
                settlementRepository.save(settlement);
            }
        }

        customer.setTotalVisits(customer.getTotalVisits() + 1);
        customerRepository.save(customer);

        checkin.setStatus(Checkin.CheckinStatus.Completed);
        checkin.setCheckoutTime(LocalDateTime.now());
        checkin.setSessionsDeducted(sessionsDeducted);
        checkin.setExtraCharges(extraCharges);
        checkin.setGstAmount(gstAmount);
        checkin.setTotalCharged(totalCharged);
        checkin.setCheckoutNotes(req.getCheckoutNotes());
        checkinRepository.save(checkin);

        return CheckoutResponse.builder()
                .checkinId(checkin.getId())
                .customerId(customer.getId())
                .parentName(customer.getParentName())
                .checkinTime(checkin.getCheckinTime())
                .checkoutTime(checkin.getCheckoutTime())
                .status(checkin.getStatus().name())
                .kidsCount(checkin.getKidsCount())
                .sessionsDeducted(sessionsDeducted)
                .balanceBefore(balanceBefore)
                .balanceAfter(customer.getGlobalSessionBalance())
                .extraCharges(extraCharges)
                .gstAmount(gstAmount)
                .totalCharged(totalCharged)
                .crossBranchSettlement(crossBranch)
                .checkoutNotes(req.getCheckoutNotes())
                .build();
    }

    // ─── Queries ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CheckinResponse> getActiveCheckins() {
        Integer branchId = BranchContext.getBranchId();
        return checkinRepository
                .findByBranchIdAndStatus(branchId, Checkin.CheckinStatus.Active)
                .stream().map(this::toCheckinResponse).toList();
    }

    @Transactional(readOnly = true)
    public CheckinResponse getCheckinById(Integer checkinId) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Checkin", checkinId));
        if (!checkin.getBranch().getId().equals(BranchContext.getBranchId()))
            throw new BranchAccessDeniedException();
        return toCheckinResponse(checkin);
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private CheckinResponse toCheckinResponse(Checkin c) {
        List<KidDto> kids = c.getCheckinKids().stream()
                .map(ck -> toKidDto(ck.getKid())).toList();
        return CheckinResponse.builder()
                .checkinId(c.getId())
                .customerId(c.getCustomer().getId())
                .parentName(c.getCustomer().getParentName())
                .phoneNumber(c.getCustomer().getPhoneNumber())
                .branchId(c.getBranch().getId())
                .branchCode(c.getBranch().getBranchCode())
                .checkinTime(c.getCheckinTime())
                .status(c.getStatus().name())
                .kidsCount(c.getKidsCount())
                .kids(kids)
                .sessionBalance(c.getCustomer().getGlobalSessionBalance())
                .build();
    }

    private KidDto toKidDto(Kid k) {
        return KidDto.builder()
                .id(k.getId())
                .kidName(k.getKidName())
                .dob(k.getDob())
                .ageInYears(k.getAgeInYears())
                .gender(k.getGender())
                .isActive(k.isActive())
                .eligible(k.isEligible())
                .build();
    }
}