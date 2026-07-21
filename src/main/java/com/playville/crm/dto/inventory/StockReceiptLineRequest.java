package com.playville.crm.dto.inventory;

import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal; import java.time.LocalDate;

@Getter @Setter
public class StockReceiptLineRequest {
    @NotNull(message = "SKU ID is required") private Integer skuId;
    @Size(max = 100) private String batchNumber;
    private LocalDate manufacturedOn;
    private LocalDate expiresOn;
    @NotNull(message = "Quantity is required") @DecimalMin(value = "0.001", message = "Quantity must be greater than zero") private BigDecimal quantity;
    @DecimalMin(value = "0.0", inclusive = true, message = "Unit cost cannot be negative") private BigDecimal unitCost;
}
