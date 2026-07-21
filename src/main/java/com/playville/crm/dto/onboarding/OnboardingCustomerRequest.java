package com.playville.crm.dto.onboarding;
import com.playville.crm.entity.enums.LeadSource;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
@Getter @Setter
public class OnboardingCustomerRequest {
 @NotBlank(message = "Parent name is required") private String parentName;
 @NotBlank(message = "Phone number is required") @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must contain 10 to 15 digits") private String phoneNumber;
 @Email(message = "Enter a valid email address") private String email;
 private LeadSource leadSource;
 private String emergencyContactName;
 private String emergencyContactPhone;
 private Boolean marketingConsent;
 private Boolean disclaimerAccepted;
 private String disclaimerVersion;
}
