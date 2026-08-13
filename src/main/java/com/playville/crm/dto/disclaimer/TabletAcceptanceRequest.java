package com.playville.crm.dto.disclaimer;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter public class TabletAcceptanceRequest {
    @NotBlank private String signerName;
    @NotBlank private String signerRelationship;
    @NotNull private Boolean confirmedReadAndAccepted;
    @NotBlank private String signatureDataUrl;
}
