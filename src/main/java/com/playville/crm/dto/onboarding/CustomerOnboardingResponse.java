package com.playville.crm.dto.onboarding;
import com.playville.crm.dto.customer.CustomerDto; import com.playville.crm.dto.kid.KidDto; import com.playville.crm.dto.trial.*; import lombok.*; import java.util.List;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor public class CustomerOnboardingResponse { private CustomerDto customer; private List<KidDto> kids; private TrialEligibilityResponse trialEligibility; private EntitlementResponse issuedEntitlement; private String nextAction; }
