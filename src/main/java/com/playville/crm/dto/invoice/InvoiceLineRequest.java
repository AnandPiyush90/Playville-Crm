package com.playville.crm.dto.invoice;

import com.playville.crm.entity.enums.InvoiceLineType;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class InvoiceLineRequest {
    @NotNull(message = "Line type is required") private InvoiceLineType lineType;
    private Integer skuId;
    private Integer packageId;
    @NotNull(message = "Quantity is required") @DecimalMin(value = "0.001", message = "Quantity must be greater than zero") private BigDecimal quantity;
}
