package com.playville.crm.dto.report;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TrialConversionResponse {
    private Integer entitlementId;
    private Integer checkinId;
    private Integer customerId;
    private String parentName;
    private String phoneNumber;
    private String campaignCode;
    private String leadSource;
    private String conversionOutcome;
    private String conversionReason;
    private Integer purchaseId;
    private BigDecimal amountPaid;
    private LocalDateTime trialIssuedAt;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private LocalDateTime followUpAt;
}
