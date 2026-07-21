package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.config.FeatureFlagService;
import com.playville.crm.dto.trial.*;
import com.playville.crm.entity.*;
import com.playville.crm.entity.enums.*;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TrialEntitlementService {
    private final CustomerRepository customers;
    private final KidRepository kids;
    private final BranchRepository branches;
    private final CustomerEntitlementRepository entitlements;
    private final EntitlementTransactionRepository transactions;
    private final CheckinRepository checkins;
    private final FeatureFlagService featureFlags;

    @Transactional(readOnly = true)
    public TrialEligibilityResponse eligibility(Integer customerId, List<Integer> kidIds) {
        featureFlags.requireTrialConversionFlow();
        Customer customer = customer(customerId);
        verifyBranch(customer);
        verifyKids(customer, kidIds);
        Optional<CustomerEntitlement> active = entitlements.findByCustomerIdAndStatusAndType(customerId, EntitlementStatus.ACTIVE, EntitlementType.COMPLIMENTARY_TRIAL).stream().findFirst();
        if (!customer.isActive()) return result(false, kidIds, "CUSTOMER_INACTIVE", "Customer is inactive", null);
        if (!customer.isDisclaimerAccepted()) return result(false, kidIds, "WAIVER_REQUIRED", "A signed waiver is required", null);
        if (checkins.findByCustomerIdAndStatus(customerId, Checkin.CheckinStatus.Active).isPresent()) return result(false, kidIds, "ACTIVE_CHECKIN", "Customer already has an active check-in", null);
        if (active.isPresent()) return result(false, kidIds, "TRIAL_ALREADY_ACTIVE", "An active trial entitlement already exists", active.get().getId());
        if (entitlements.existsByCustomerIdAndType(customerId, EntitlementType.COMPLIMENTARY_TRIAL)) return result(false, kidIds, "TRIAL_ALREADY_USED", "This family has already used its first-visit trial", null);
        if (customer.getGlobalSessionBalance() > 0 || customer.getCurrentPackage() != null) return result(false, kidIds, "PREVIOUS_PAID_PURCHASE", "Existing paid customers are not eligible for a first-visit trial", null);
        return result(true, kidIds, "FIRST_VISIT", "Eligible for one family complimentary trial", null);
    }

    @Transactional
    public EntitlementResponse issue(Integer customerId, IssueTrialRequest request) {
        featureFlags.requireTrialConversionFlow();
        Customer lockedCustomer = customers.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        verifyBranch(lockedCustomer);
        TrialEligibilityResponse eligibility = eligibility(customerId, request.getKidIds());
        if (!eligibility.isEligible()) throw new BusinessRuleException(eligibility.getReasonCode() + ": " + eligibility.getReason());
        Branch branch = branches.findById(BranchContext.getBranchId()).orElseThrow(() -> new ResourceNotFoundException("Branch", BranchContext.getBranchId()));
        CustomerEntitlement entitlement = CustomerEntitlement.builder().customer(lockedCustomer).branch(branch)
                .type(EntitlementType.COMPLIMENTARY_TRIAL).status(EntitlementStatus.ACTIVE).sessionsGranted(1)
                .campaignCode(request.getCampaignCode() == null ? "FIRST_VISIT" : request.getCampaignCode())
                .reasonText(request.getNotes()).expiresAt(endOfBranchBusinessDay()).build();
        CustomerEntitlement saved = entitlements.save(entitlement);
        record(saved, null, EntitlementTransactionType.ISSUED, 1, 0, request.getNotes());
        return dto(saved);
    }

    @Transactional(readOnly = true)
    public List<EntitlementResponse> activeEntitlements(Integer customerId) {
        Customer customer = customer(customerId);
        verifyBranch(customer);
        return entitlements.findByCustomerIdAndStatus(customerId, EntitlementStatus.ACTIVE).stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public List<EntitlementResponse> entitlements(Integer customerId, EntitlementStatus status, EntitlementType type) {
        featureFlags.requireTrialConversionFlow();
        Customer customer = customer(customerId);
        verifyBranch(customer);
        return entitlements.findByCustomerId(customerId).stream()
                .filter(e -> status == null || e.getStatus() == status)
                .filter(e -> type == null || e.getType() == type)
                .map(this::dto)
                .toList();
    }

    @Transactional
    public EntitlementResponse grantManualComplimentary(Integer customerId, ManualComplimentaryRequest request) {
        featureFlags.requireTrialConversionFlow();
        Customer customer = customer(customerId);
        verifyBranch(customer);
        Branch branch = branches.findById(BranchContext.getBranchId()).orElseThrow(() -> new ResourceNotFoundException("Branch", BranchContext.getBranchId()));
        CustomerEntitlement saved = entitlements.save(CustomerEntitlement.builder().customer(customer).branch(branch)
                .type(EntitlementType.MANUAL_COMPLIMENTARY).status(EntitlementStatus.ACTIVE).sessionsGranted(1)
                .campaignCode(request.getCampaignCode() == null ? "MANUAL_COMP" : request.getCampaignCode())
                .reasonText(request.getReason()).expiresAt(endOfBranchBusinessDay()).build());
        record(saved, null, EntitlementTransactionType.MANUAL_ADJUSTMENT, 1, 0, request.getReason());
        return dto(saved);
    }

    @Transactional
    public EntitlementResponse cancel(Integer entitlementId, ReasonRequest request) {
        featureFlags.requireTrialConversionFlow();
        CustomerEntitlement entitlement = entitlements.findByIdForUpdate(entitlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Entitlement", entitlementId));
        if (!entitlement.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
        if (entitlement.getStatus() == EntitlementStatus.RESERVED) throw new BusinessRuleException("A reserved entitlement must be released by cancelling its check-in");
        if (entitlement.getStatus() != EntitlementStatus.ACTIVE) throw new BusinessRuleException("Only active entitlements can be cancelled");
        entitlement.setStatus(EntitlementStatus.CANCELLED);
        record(entitlement, null, EntitlementTransactionType.CANCELLED, -entitlement.getSessionsGranted(), 0, request.getReason());
        return dto(entitlement);
    }

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void expireDueEntitlements() {
        LocalDateTime now = LocalDateTime.now();
        for (CustomerEntitlement candidate : entitlements.findByStatusAndExpiresAtBefore(EntitlementStatus.ACTIVE, now)) {
            entitlements.findByIdForUpdate(candidate.getId()).ifPresent(entitlement -> {
                if (entitlement.getStatus() == EntitlementStatus.ACTIVE && !entitlement.getExpiresAt().isAfter(now)) {
                    entitlement.setStatus(EntitlementStatus.EXPIRED);
                    record(entitlement, null, EntitlementTransactionType.EXPIRED, -entitlement.getSessionsGranted(), 0, "Expired automatically");
                }
            });
        }
    }

    @Transactional
    public CustomerEntitlement reserve(Integer customerId, Integer entitlementId) {
        CustomerEntitlement entitlement = entitlements.findByIdForUpdate(entitlementId).orElseThrow(() -> new ResourceNotFoundException("Entitlement", entitlementId));
        if (!entitlement.getCustomer().getId().equals(customerId) || !entitlement.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
        if (entitlement.getStatus() != EntitlementStatus.ACTIVE || !entitlement.getExpiresAt().isAfter(LocalDateTime.now())) throw new BusinessRuleException("Entitlement is not active or has expired");
        entitlement.setStatus(EntitlementStatus.RESERVED); entitlement.setSessionsReserved(1);
        record(entitlement, null, EntitlementTransactionType.RESERVED, 0, 1, "Reserved at check-in");
        return entitlement;
    }

    @Transactional public void consume(CustomerEntitlement entitlement, Checkin checkin) { updateReservation(entitlement, checkin, EntitlementStatus.CONSUMED, EntitlementTransactionType.CONSUMED, "Consumed at checkout"); }
    @Transactional public void release(CustomerEntitlement entitlement, Checkin checkin, String notes) { updateReservation(entitlement, checkin, EntitlementStatus.ACTIVE, EntitlementTransactionType.RELEASED, notes); }
    private void updateReservation(CustomerEntitlement input, Checkin checkin, EntitlementStatus status, EntitlementTransactionType transactionType, String notes) { CustomerEntitlement e = entitlements.findByIdForUpdate(input.getId()).orElseThrow(); if (e.getStatus() == EntitlementStatus.RESERVED) { e.setStatus(status); e.setSessionsReserved(0); record(e, checkin, transactionType, status == EntitlementStatus.CONSUMED ? -1 : 0, -1, notes); } }
    private void record(CustomerEntitlement entitlement, Checkin checkin, EntitlementTransactionType type, int sessionsDelta, int reservedDelta, String notes) { transactions.save(EntitlementTransaction.builder().entitlement(entitlement).checkin(checkin).transactionType(type).sessionsDelta(sessionsDelta).reservedDelta(reservedDelta).notes(notes).build()); }
    public EntitlementResponse dto(CustomerEntitlement e) { return EntitlementResponse.builder().id(e.getId()).type(e.getType().name()).status(e.getStatus().name()).sessionsGranted(e.getSessionsGranted()).sessionsReserved(e.getSessionsReserved()).campaignCode(e.getCampaignCode()).expiresAt(e.getExpiresAt()).build(); }
    private Customer customer(Integer id) { return customers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", id)); }
    private void verifyBranch(Customer customer) { if (!customer.getHomeBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException(); }
    private void verifyKids(Customer customer, List<Integer> ids) { for (Integer id : ids) { Kid kid = kids.findById(id).orElseThrow(() -> new ResourceNotFoundException("Kid", id)); if (!kid.getCustomer().getId().equals(customer.getId()) || !kid.isEligible()) throw new BusinessRuleException("KID_INELIGIBLE: Kid is not eligible for this customer"); } }
    private TrialEligibilityResponse result(boolean eligible, List<Integer> ids, String code, String reason, Integer id) { return TrialEligibilityResponse.builder().eligible(eligible).scope("FAMILY").eligibleKidIds(ids).reasonCode(code).reason(reason).existingEntitlementId(id).build(); }
    private LocalDateTime endOfBranchBusinessDay() { return LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1).atStartOfDay(); }
}
