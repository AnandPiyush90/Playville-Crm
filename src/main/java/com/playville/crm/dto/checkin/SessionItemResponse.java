package com.playville.crm.dto.checkin;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class SessionItemResponse {
    private Integer id;
    private Integer skuId;
    private String productName;
    private String skuCode;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal taxRate;
    private BigDecimal taxableAmount;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;
    private String status;
    private String addedByStaffName;
    private String notes;
    private LocalDateTime addedAt;
}
