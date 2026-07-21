package com.playville.crm.dto.inventory;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;
@Getter @Setter public class StockTransferLineRequest { @NotNull(message = "SKU ID is required") private Integer skuId; private Integer batchId; @NotNull(message = "Quantity is required") @DecimalMin(value = "0.001", message = "Quantity must be greater than zero") private BigDecimal quantity; }
