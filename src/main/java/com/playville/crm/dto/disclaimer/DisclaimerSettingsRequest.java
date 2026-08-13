package com.playville.crm.dto.disclaimer;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter
public class DisclaimerSettingsRequest {
     private Boolean disclaimerRequiredForPhysicalVisit;
     private Boolean tabletSignatureEnabled;
     private Boolean emailConfirmationEnabled;
     @Min(1) @Max(72) private Integer emailLinkTtlHours;
     private Boolean disclaimerResignOnNewVersion;
     private Long activeDisclaimerTemplateId;
}
