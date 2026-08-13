package com.playville.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playville.crm.entity.Branch;
import com.playville.crm.entity.Customer;
import com.playville.crm.entity.DisclaimerAcceptance;
import com.playville.crm.entity.DisclaimerTemplate;
import com.playville.crm.repository.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DisclaimerServiceTest {
    private final DisclaimerService service = new DisclaimerService(
            mock(DisclaimerTemplateRepository.class), mock(CustomerOnboardingDraftRepository.class),
            mock(DisclaimerSigningRequestRepository.class), mock(DisclaimerAcceptanceRepository.class),
            mock(BranchRepository.class), new ObjectMapper(), mock(BranchEmailService.class), mock(NotificationDeliveryService.class), mock(EmailTemplateService.class));

    @Test
    void disabledPolicyDoesNotBlockCustomer() {
        Branch branch = Branch.builder().disclaimerRequiredForPhysicalVisit(false).build();
        assertThat(service.current(new Customer(), branch)).isTrue();
    }

    @Test
    void enabledPolicyRequiresAValidAcceptance() {
        Branch branch = Branch.builder().id(1).disclaimerRequiredForPhysicalVisit(true).build();
        assertThat(service.current(new Customer(), branch)).isFalse();
    }

    @Test
    void newActiveVersionRequiresResigningWhenConfigured() {
        DisclaimerTemplate oldTemplate = DisclaimerTemplate.builder().id(10L).build();
        DisclaimerTemplate activeTemplate = DisclaimerTemplate.builder().id(11L).build();
        DisclaimerAcceptance acceptance = DisclaimerAcceptance.builder().template(oldTemplate).status("VALID").build();
        Customer customer = Customer.builder().currentDisclaimerAcceptance(acceptance).build();
        Branch branch = Branch.builder().id(1).disclaimerRequiredForPhysicalVisit(true)
                .disclaimerResignOnNewVersion(true).activeDisclaimerTemplate(activeTemplate).build();

        assertThat(service.current(customer, branch)).isFalse();
    }
}
