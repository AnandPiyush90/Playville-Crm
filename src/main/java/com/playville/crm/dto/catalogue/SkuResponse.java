package com.playville.crm.dto.catalogue;

import lombok.Builder; import lombok.Getter;
import java.math.BigDecimal;
@Getter @Builder public class SkuResponse { private Integer id; private String skuCode; private String barcode; private String variantAttributesJson; private String unitOfMeasure; private BigDecimal salePrice; private BigDecimal defaultSalePrice; private BigDecimal defaultCostPrice; private Integer taxProfileId; private BigDecimal availableQuantity; private boolean availableAtBranch; private boolean hasExpiry; private boolean active; }
