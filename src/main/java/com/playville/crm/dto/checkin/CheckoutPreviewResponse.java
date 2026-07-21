package com.playville.crm.dto.checkin;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder
public class CheckoutPreviewResponse {
    private Integer checkinId;
    private Integer customerId;
    private String visitType;
    private Integer entitlementId;
    private String entitlementStatus;
    private LocalDateTime entitlementExpiresAt;
    private int kidsCount;
    private Integer paidSessionBalance;
    private BigDecimal extraCharges;
    private List<SessionItemResponse> sessionItems;
    private BigDecimal itemsTaxableAmount;
    private BigDecimal itemsTaxAmount;
    private BigDecimal itemsTotal;
}
