package com.playville.crm.dto.invoice;

import com.playville.crm.entity.enums.InvoiceLineType;
import lombok.Builder; import lombok.Getter;
import java.math.BigDecimal;
@Getter @Builder public class InvoiceLineResponse {
    private Integer id;
    private Integer lineNumber;
    private InvoiceLineType lineType;
    private Integer productId;
    private Integer skuId;
    private Integer packageId;
    private Integer purchaseId;
    private Integer sessionsIncluded;
    private Integer validityDays;
    private String description;
    private String skuCode;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal grossAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxableAmount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;
    private BigDecimal returnedQuantity;
    private BigDecimal availableQuantity;
}
