package com.playville.crm.dto.disclaimer;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter public class TemplateRequest {
    @NotBlank private String templateCode;
    @NotBlank private String version;
    @NotBlank private String title;
    @NotBlank private String contentHtml;
}
