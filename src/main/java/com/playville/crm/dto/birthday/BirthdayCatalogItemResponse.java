package com.playville.crm.dto.birthday;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter @Builder
public class BirthdayCatalogItemResponse {
    private Integer branchConfigurationId;
    private Integer id;
    private String itemCode;
    private String category;
    private String itemName;
    private String unitLabel;
    private BigDecimal unitPrice;
    private BigDecimal taxRate;
    private Integer skuId;
    private String skuCode;
    private String description;
    private String inventoryMode;
    private BigDecimal minimumOrderQuantity;
    private Integer leadTimeDays;
    private boolean active;
    private boolean available;
    private boolean reserveInventory;
    private BigDecimal availableQuantity;
    private BigDecimal baseUnitPrice;
    private BigDecimal branchPriceOverride;
    private String displayNameOverride;
}
