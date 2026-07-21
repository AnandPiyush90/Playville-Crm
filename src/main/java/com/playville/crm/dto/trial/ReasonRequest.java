package com.playville.crm.dto.trial;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReasonRequest {
    @NotBlank(message = "A reason is required")
    private String reason;
}
