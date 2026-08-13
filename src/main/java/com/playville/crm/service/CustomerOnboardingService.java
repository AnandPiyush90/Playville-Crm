package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.config.FeatureFlagService;
import com.playville.crm.dto.customer.CustomerDto;
import com.playville.crm.dto.kid.KidDto;
import com.playville.crm.dto.onboarding.*;
import com.playville.crm.dto.trial.*;
import com.playville.crm.entity.*;
import com.playville.crm.entity.enums.LeadSource;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class CustomerOnboardingService {
    private final CustomerRepository customers; private final KidRepository kids; private final BranchRepository branches;
    private final OnboardingIdempotencyRepository idempotency; private final CustomerEntitlementRepository entitlements;
    private final DisclaimerAcceptanceRepository disclaimerAcceptances;
    private final TrialEntitlementService trials; private final FeatureFlagService featureFlags;

    @Transactional
    public CustomerOnboardingResponse onboard(CustomerOnboardingRequest request, String key) {
        featureFlags.requireTrialConversionFlow();
        if (key == null || key.isBlank()) throw new BusinessRuleException("Idempotency-Key header is required");
        Optional<OnboardingIdempotency> replay = idempotency.findById(key);
        if (replay.isPresent()) return replay(replay.get());
        OnboardingIdempotency claim = claimKey(key);
        String phone = normalizePhone(request.getCustomer().getPhoneNumber());
        customers.findByPhoneNumber(phone).ifPresent(existing -> { throw new CustomerPhoneExistsException(existing.getId()); });
        Branch branch = branches.findById(BranchContext.getBranchId()).orElseThrow(() -> new ResourceNotFoundException("Branch", BranchContext.getBranchId()));
        DisclaimerAcceptance signedAcceptance = requiredAcceptance(request, branch, phone);
        Customer customer = customers.save(Customer.builder().parentName(request.getCustomer().getParentName()).phoneNumber(phone)
                .email(request.getCustomer().getEmail()).leadSource(request.getCustomer().getLeadSource() == null ? LeadSource.Walk_in : request.getCustomer().getLeadSource())
                .emergencyContactName(request.getCustomer().getEmergencyContactName()).emergencyContactPhone(request.getCustomer().getEmergencyContactPhone())
                .marketingConsent(Boolean.TRUE.equals(request.getCustomer().getMarketingConsent())).disclaimerAccepted(signedAcceptance != null)
                .disclaimerVersion(signedAcceptance == null ? null : signedAcceptance.getTemplateVersionSnapshot()).acceptanceTimestamp(signedAcceptance == null ? null : signedAcceptance.getAcceptedAt())
                .homeBranch(branch).firstVisitBranch(branch).build());
        if(signedAcceptance!=null){
            signedAcceptance.setCustomer(customer); customer.setCurrentDisclaimerAcceptance(signedAcceptance);
            signedAcceptance.getDraft().setCompletedCustomer(customer); signedAcceptance.getDraft().setStatus("COMPLETED");
        }
        List<Kid> createdKids = new ArrayList<>();
        for (OnboardingKidRequest child : request.getKids()) {
            int age = Period.between(child.getDob(), LocalDate.now()).getYears();
            if (age < 0 || age > 8) throw new BusinessRuleException("KID_INELIGIBLE: Child must be between 0 and 8 years old");
            createdKids.add(kids.save(Kid.builder().customer(customer).branch(branch).kidName(child.getKidName()).dob(child.getDob()).gender(child.getGender()).specialNotes(child.getSpecialNotes()).build()));
        }
        CustomerEntitlement entitlement = null;
        if ("COMPLIMENTARY_TRIAL".equals(request.getVisitPurpose())) {
            IssueTrialRequest issue = new IssueTrialRequest(); issue.setKidIds(createdKids.stream().map(Kid::getId).toList());
            if (request.getTrial() != null) { issue.setCampaignCode(request.getTrial().getCampaignCode()); issue.setNotes(request.getTrial().getNotes()); }
            EntitlementResponse granted = trials.issue(customer.getId(), issue);
            entitlement = entitlements.getReferenceById(granted.getId());
        }
        String nextAction = "COMPLIMENTARY_TRIAL".equals(request.getVisitPurpose()) ? "CHECK_IN" : "BUY_PACKAGE_NOW".equals(request.getVisitPurpose()) ? "PURCHASE" : "CUSTOMER_DETAIL";
        claim.setCustomer(customer);
        claim.setEntitlement(entitlement);
        claim.setNextAction(nextAction);
        claim.setStatus("COMPLETED");
        idempotency.save(claim);
        return response(customer, entitlement, nextAction);
    }

    private OnboardingIdempotency claimKey(String key) {
        try {
            return idempotency.saveAndFlush(OnboardingIdempotency.builder()
                    .idempotencyKey(key)
                    .status("PROCESSING")
                    .build());
        } catch (DataIntegrityViolationException ex) {
            return idempotency.findById(key).map(this::requireCompleted)
                    .orElseThrow(() -> new BusinessRuleException("Duplicate request is still being processed; please retry"));
        }
    }

    private CustomerOnboardingResponse replay(OnboardingIdempotency entry) {
        return response(requireCompleted(entry).getCustomer(), entry.getEntitlement(), entry.getNextAction());
    }

    private OnboardingIdempotency requireCompleted(OnboardingIdempotency entry) {
        if (!"COMPLETED".equals(entry.getStatus()) || entry.getCustomer() == null) {
            throw new BusinessRuleException("Duplicate request is still being processed; please retry");
        }
        return entry;
    }

    private CustomerOnboardingResponse response(Customer c, CustomerEntitlement entitlement, String nextAction) {
        List<KidDto> childDtos = kids.findByCustomerIdAndIsActiveTrue(c.getId()).stream().map(this::kidDto).toList();
        CustomerDto customerDto = CustomerDto.builder().id(c.getId()).parentName(c.getParentName()).phoneNumber(c.getPhoneNumber()).email(c.getEmail()).leadSource(c.getLeadSource()).globalSessionBalance(c.getGlobalSessionBalance()).homeBranchId(c.getHomeBranch().getId()).homeBranchCode(c.getHomeBranch().getBranchCode()).disclaimerAccepted(c.isDisclaimerAccepted()).totalVisits(c.getTotalVisits()).isActive(c.isActive()).kids(childDtos).createdAt(c.getCreatedAt()).build();
        TrialEligibilityResponse eligibility = entitlement == null ? null : TrialEligibilityResponse.builder().eligible(false).scope("FAMILY").eligibleKidIds(childDtos.stream().map(KidDto::getId).toList()).reasonCode("TRIAL_ALREADY_ACTIVE").reason("Trial entitlement issued").existingEntitlementId(entitlement.getId()).build();
        return CustomerOnboardingResponse.builder().customer(customerDto).kids(childDtos).trialEligibility(eligibility).issuedEntitlement(entitlement == null ? null : trials.dto(entitlement)).nextAction(nextAction).build();
    }
    private KidDto kidDto(Kid k) { return KidDto.builder().id(k.getId()).kidName(k.getKidName()).dob(k.getDob()).ageInYears(k.getAgeInYears()).gender(k.getGender()).specialNotes(k.getSpecialNotes()).isActive(k.isActive()).eligible(k.isEligible()).createdAt(k.getCreatedAt()).build(); }
    private String normalizePhone(String value) { return value.replaceAll("[^0-9]", ""); }
    private boolean isPhysicalVisit(String purpose) { return "COMPLIMENTARY_TRIAL".equals(purpose) || "BUY_PACKAGE_NOW".equals(purpose); }
    private DisclaimerAcceptance requiredAcceptance(CustomerOnboardingRequest request, Branch branch, String phone) {
        if(!isPhysicalVisit(request.getVisitPurpose()) || !branch.isDisclaimerRequiredForPhysicalVisit()) return null;
        Long id=request.getCustomer().getDisclaimerAcceptanceId();
        if(id==null)throw new BusinessRuleException("DISCLAIMER_REQUIRED: Complete tablet signature or email consent before onboarding");
        DisclaimerAcceptance a=disclaimerAcceptances.findById(id).orElseThrow(()->new BusinessRuleException("DISCLAIMER_REQUIRED: Signed acceptance was not found"));
        boolean valid="VALID".equals(a.getStatus())&&a.getCustomer()==null&&a.getBranch().getId().equals(branch.getId())
                &&"SIGNED".equals(a.getDraft().getStatus())&&phone.equals(a.getDraft().getPhoneNumber())
                &&(!branch.isDisclaimerResignOnNewVersion()||branch.getActiveDisclaimerTemplate()!=null&&a.getTemplate().getId().equals(branch.getActiveDisclaimerTemplate().getId()));
        if(!valid)throw new BusinessRuleException("DISCLAIMER_REQUIRED: The signed acceptance is not valid for this onboarding");
        return a;
    }
}
