package com.playville.crm.dto.checkin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CancelCheckinRequest {
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
