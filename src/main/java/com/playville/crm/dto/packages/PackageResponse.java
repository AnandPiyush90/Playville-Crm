package com.playville.crm.dto.packages;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class PackageResponse {
    private Integer    id;
    private String     packageName;
    private Integer    sessionsPurchased;
    private Integer    sessionsBonus;
    private Integer    sessionsTotal;
    private BigDecimal pricePerSession;
    private BigDecimal totalPrice;
    private Integer    validityDays;
    private BigDecimal birthdayDiscountPct;
    private BigDecimal rechargeDiscountPct;
    private boolean    canUpgrade;
    private boolean    isActive;
    private Integer    displayOrder;
    private LocalDateTime createdAt;
}