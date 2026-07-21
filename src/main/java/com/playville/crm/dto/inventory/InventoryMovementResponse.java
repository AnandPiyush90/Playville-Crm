package com.playville.crm.dto.inventory;

import com.playville.crm.entity.enums.InventoryMovementType;
import lombok.Builder; import lombok.Getter;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Getter @Builder public class InventoryMovementResponse { private Integer id; private Integer skuId; private InventoryMovementType movementType; private BigDecimal quantityDelta; private String referenceType; private Integer referenceId; private String reasonCode; private String notes; private LocalDateTime createdAt; }
