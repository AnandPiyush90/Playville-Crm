package com.playville.crm.service;
import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.birthday.*;
import com.playville.crm.entity.*;
import com.playville.crm.exception.ResourceNotFoundException;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class BirthdayBranchPolicyService {
    private final BirthdayBranchPolicyRepository policyRepository;
    private final BranchRepository branchRepository;
    @Transactional public BirthdayBranchPolicyResponse get() { return response(policy()); }
    @Transactional public BirthdayBranchPolicyResponse update(BirthdayBranchPolicyRequest request) {
        BirthdayBranchPolicy policy = policy();
        if (request.getExtraKidPrice() != null) policy.setExtraKidPrice(request.getExtraKidPrice());
        if (request.getExtraAdultPrice() != null) policy.setExtraAdultPrice(request.getExtraAdultPrice());
        if (request.getExtraTime30MinPrice() != null) policy.setExtraTime30MinPrice(request.getExtraTime30MinPrice());
        if (request.getEnquiryCalendarEnabled() != null) policy.setEnquiryCalendarEnabled(request.getEnquiryCalendarEnabled());
        if (request.getCancellationPolicyJson() != null) policy.setCancellationPolicyJson(request.getCancellationPolicyJson());
        if (request.getRefundPolicyJson() != null) policy.setRefundPolicyJson(request.getRefundPolicyJson());
        if (request.getShareChannel() != null) policy.setShareChannel(request.getShareChannel());
        if (request.getShareSettingsJson() != null) policy.setShareSettingsJson(request.getShareSettingsJson());
        return response(policyRepository.save(policy));
    }
    private BirthdayBranchPolicy policy() { Integer branchId = BranchContext.getBranchId(); return policyRepository.findByBranchId(branchId).orElseGet(() -> policyRepository.save(BirthdayBranchPolicy.builder().branch(branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch", branchId))).build())); }
    private BirthdayBranchPolicyResponse response(BirthdayBranchPolicy value) { return BirthdayBranchPolicyResponse.builder().branchId(value.getBranch().getId()).extraKidPrice(value.getExtraKidPrice()).extraAdultPrice(value.getExtraAdultPrice()).extraTime30MinPrice(value.getExtraTime30MinPrice()).enquiryCalendarEnabled(value.getEnquiryCalendarEnabled()).cancellationPolicyJson(value.getCancellationPolicyJson()).refundPolicyJson(value.getRefundPolicyJson()).shareChannel(value.getShareChannel()).shareSettingsJson(value.getShareSettingsJson()).build(); }
}
