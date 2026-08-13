package com.playville.crm.dto.notification;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @Builder
public class EmailProviderConfigResponse {
    private Long id; private String providerType, host, username, tlsMode, fromEmail, fromName, replyToEmail;
    private int port; private boolean enabled, passwordConfigured;
    private String lastTestStatus, lastErrorCode; private LocalDateTime lastTestedAt;
}
