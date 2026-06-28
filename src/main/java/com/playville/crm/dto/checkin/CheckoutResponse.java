package com.playville.crm.dto.checkin;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}