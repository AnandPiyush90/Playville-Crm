package com.playville.crm.dto.disclaimer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmailAcceptanceRequest {
    @NotBlank private String signerName;
    @NotBlank private String signerRelationship;
    @NotNull private Boolean confirmedReadAndAccepted;
}
