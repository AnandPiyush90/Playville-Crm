package com.playville.crm.dto.checkin;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder
public class CheckoutResponse {
    private Integer       checkinId;
    private Integer       customerId;
    private String        parentName;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private String        status;
    private Integer       kidsCount;
    private Integer       sessionsDeducted;
    private Integer       balanceBefore;
    private Integer       balanceAfter;
    private BigDecimal    extraCharges;
    private BigDecimal    gstAmount;
    private BigDecimal    totalCharged;
    private boolean       crossBranchSettlement;
    private String        checkoutNotes;
    private String        conversionOutcome;
    private String        conversionReason;
    private Integer       purchaseId;
    private List<SessionItemResponse> sessionItems;
    private BigDecimal itemsTotal;
}
