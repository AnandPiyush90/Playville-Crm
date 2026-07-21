package com.playville.crm.dto.inventory;

import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.time.LocalDateTime; import java.util.List;

@Getter @Setter
public class CreateStockReceiptRequest {
    private Integer supplierId;
    @Size(max = 100) private String supplierInvoiceReference;
    private LocalDateTime receivedAt;
    private String notes;
    @NotEmpty(message = "At least one receipt item is required") @Valid private List<StockReceiptLineRequest> items;
}
