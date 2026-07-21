package com.playville.crm.service;

import com.playville.crm.config.FeatureFlagService;
import com.playville.crm.context.BranchContext;
import com.playville.crm.entity.*;
import com.playville.crm.exception.BranchAccessDeniedException;
import com.playville.crm.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrialEntitlementServiceTest {

    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final KidRepository kids = mock(KidRepository.class);
    private final BranchRepository branches = mock(BranchRepository.class);
    private final CustomerEntitlementRepository entitlements = mock(CustomerEntitlementRepository.class);
    private final EntitlementTransactionRepository transactions = mock(EntitlementTransactionRepository.class);
    private final CheckinRepository checkins = mock(CheckinRepository.class);
    private final FeatureFlagService featureFlags = new FeatureFlagService(true);

    private final TrialEntitlementService service = new TrialEntitlementService(
            customers, kids, branches, entitlements, transactions, checkins, featureFlags);

    @AfterEach
    void clearBranchContext() {
        BranchContext.clear();
    }

    @Test
    void eligibilityRejectsCustomerFromAnotherBranch() {
        BranchContext.set(1, "BR1");
        Branch otherBranch = Branch.builder().id(2).branchCode("BR2").branchName("Other").build();
        Customer customer = Customer.builder().id(10).phoneNumber("9999999999").parentName("Parent")
                .homeBranch(otherBranch).disclaimerAccepted(true).isActive(true).build();

        when(customers.findById(10)).thenReturn(Optional.of(customer));

        assertThrows(BranchAccessDeniedException.class,
                () -> service.eligibility(10, List.of(100)));
        verifyNoInteractions(kids);
    }

    @Test
    void eligibilityReturnsActiveCheckinReason() {
        BranchContext.set(1, "BR1");
        Branch branch = Branch.builder().id(1).branchCode("BR1").branchName("Main").build();
        Customer customer = Customer.builder().id(10).phoneNumber("9999999999").parentName("Parent")
                .homeBranch(branch).disclaimerAccepted(true).isActive(true).build();
        Kid kid = Kid.builder().id(100).customer(customer).branch(branch)
                .kidName("Kid").dob(java.time.LocalDate.now().minusYears(4)).build();
        Checkin active = Checkin.builder().id(50).customer(customer).branch(branch).status(Checkin.CheckinStatus.Active).build();

        when(customers.findById(10)).thenReturn(Optional.of(customer));
        when(kids.findById(100)).thenReturn(Optional.of(kid));
        when(entitlements.findByCustomerIdAndStatusAndType(10, com.playville.crm.entity.enums.EntitlementStatus.ACTIVE,
                com.playville.crm.entity.enums.EntitlementType.COMPLIMENTARY_TRIAL)).thenReturn(List.of());
        when(checkins.findByCustomerIdAndStatus(10, Checkin.CheckinStatus.Active)).thenReturn(Optional.of(active));

        var response = service.eligibility(10, List.of(100));

        org.junit.jupiter.api.Assertions.assertFalse(response.isEligible());
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE_CHECKIN", response.getReasonCode());
    }
}
