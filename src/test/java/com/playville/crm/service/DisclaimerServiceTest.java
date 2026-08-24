package com.playville.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.disclaimer.DisclaimerView;
import com.playville.crm.entity.Branch;
import com.playville.crm.entity.Customer;
import com.playville.crm.entity.DisclaimerAcceptance;
import com.playville.crm.entity.DisclaimerTemplate;
import com.playville.crm.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DisclaimerServiceTest {
    private final DisclaimerTemplateRepository templates = mock(DisclaimerTemplateRepository.class);
    private final BranchRepository branches = mock(BranchRepository.class);
    private final DisclaimerService service = new DisclaimerService(
            templates, mock(CustomerOnboardingDraftRepository.class),
            mock(DisclaimerSigningRequestRepository.class), mock(DisclaimerAcceptanceRepository.class),
            branches, new ObjectMapper(), mock(BranchEmailService.class), mock(NotificationDeliveryService.class),
            mock(EmailTemplateService.class), mock(PlayvilleDisclaimerCopy.class));

    @AfterEach
    void clearContext() {
        BranchContext.clear();
    }

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

    @Test
    void publishAssignsTheTemplateAsActiveOnTheBranch() {
        BranchContext.set(1, "NAL");
        Branch branch = Branch.builder().id(1).disclaimerRequiredForPhysicalVisit(true).tabletSignatureEnabled(true).build();
        DisclaimerTemplate draft = DisclaimerTemplate.builder().id(9L).branch(branch).status("DRAFT")
                .templateCode("PLAYVILLE_WAIVER").version("1.0").title("PlayVille Guardian Disclaimer")
                .contentHtml("<p>I accept the risks of indoor play.</p>").build();
        when(templates.findById(9L)).thenReturn(Optional.of(draft));
        when(branches.findByIdWithDisclaimerTemplate(1)).thenReturn(Optional.of(branch));
        when(branches.save(branch)).thenReturn(branch);

        DisclaimerView published = service.publish(9L);

        assertThat(published.getStatus()).isEqualTo("PUBLISHED");
        assertThat(draft.getStatus()).isEqualTo("PUBLISHED");
        assertThat(draft.getContentSha256()).isNotBlank();
        assertThat(branch.getActiveDisclaimerTemplate()).isSameAs(draft);
        verify(branches).save(branch);
    }
}
