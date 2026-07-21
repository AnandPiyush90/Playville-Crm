package com.playville.crm.dto.onboarding;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import lombok.Getter; import lombok.Setter; import java.util.List;
@Getter @Setter public class CustomerOnboardingRequest { @Valid @NotNull(message = "Customer details are required") private OnboardingCustomerRequest customer; @Valid @NotEmpty(message = "At least one kid is required") private List<OnboardingKidRequest> kids; @NotBlank(message = "Visit purpose is required") private String visitPurpose; @Valid private OnboardingTrialRequest trial; }
