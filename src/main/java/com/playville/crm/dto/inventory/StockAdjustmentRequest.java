package com.playville.crm.dto.inventory;

import com.playville.crm.entity.enums.InventoryMovementType;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class StockAdjustmentRequest {
    @NotNull(message = "SKU ID is required") private Integer skuId;
    private Integer batchId;
    @NotNull(message = "Movement type is required") private InventoryMovementType movementType;
    @NotNull(message = "Quantity is required") @DecimalMin(value = "0.001", message = "Quantity must be greater than zero") private BigDecimal quantity;
    @NotBlank(message = "Reason code is required") @Size(max = 60) private String reasonCode;
    @NotBlank(message = "Notes are required") private String notes;
}
