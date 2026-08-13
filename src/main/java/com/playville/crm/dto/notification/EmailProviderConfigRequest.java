package com.playville.crm.dto.notification;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmailProviderConfigRequest {
    @NotBlank @Pattern(regexp = "GMAIL_SMTP|CUSTOM_SMTP") private String providerType;
    @Size(max = 150) private String host;
    @Min(1) @Max(65535) private Integer port;
    @NotBlank @Size(max = 150) private String username;
    @Size(max = 500) private String password;
    @Pattern(regexp = "STARTTLS|SSL_TLS") private String tlsMode;
    @Email @NotBlank @Size(max = 150) private String fromEmail;
    @NotBlank @Size(max = 150) private String fromName;
    @Email @Size(max = 150) private String replyToEmail;
    @NotNull private Boolean enabled;
}
