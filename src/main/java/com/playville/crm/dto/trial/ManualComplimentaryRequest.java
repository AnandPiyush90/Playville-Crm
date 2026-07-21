package com.playville.crm.dto.trial;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ManualComplimentaryRequest {
    @NotBlank(message = "A reason is required for a manual complimentary entitlement")
    private String reason;
    private String campaignCode;
}
