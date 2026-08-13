package com.playville.crm.dto.disclaimer;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DisclaimerSettingsResponse {
    private boolean disclaimerRequiredForPhysicalVisit;
    private boolean tabletSignatureEnabled;
    private boolean emailConfirmationEnabled;
    private int emailLinkTtlHours;
    private boolean disclaimerResignOnNewVersion;
    private Long activeDisclaimerTemplateId;
    private String activeDisclaimerTemplateTitle;
    private boolean configurationReady;
}
