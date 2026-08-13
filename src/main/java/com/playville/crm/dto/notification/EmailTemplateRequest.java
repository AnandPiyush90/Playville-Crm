package com.playville.crm.dto.notification;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class EmailTemplateRequest {
    @NotBlank @Size(max=300) private String subject;
    @NotBlank @Size(max=10000) private String bodyText;
    private Long version;
}
