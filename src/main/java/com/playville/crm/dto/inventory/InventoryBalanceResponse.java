package com.playville.crm.dto.inventory;

import lombok.Builder; import lombok.Getter;
import java.time.LocalDate;
import java.math.BigDecimal;
@Getter @Builder public class InventoryBalanceResponse {
    private Integer skuId;
    private String skuCode;
    private String productName;
    private String categoryName;
    private BigDecimal quantityOnHand;
    private BigDecimal quantityReserved;
    private BigDecimal availableQuantity;
    private BigDecimal reorderLevel;
    private boolean lowStock;
    private boolean outOfStock;
    private LocalDate nearestExpiry;
    private boolean expiringSoon;
    private boolean availableForSale;
}
