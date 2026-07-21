package com.playville.crm.dto.catalogue;

import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class UpdateBranchSkuRequest {
    @DecimalMin(value = "0.0", inclusive = true, message = "Sale price cannot be negative") private BigDecimal salePriceOverride;
    @DecimalMin(value = "0.0", inclusive = true, message = "Reorder level cannot be negative") private BigDecimal reorderLevel;
    @DecimalMin(value = "0.0", inclusive = true, message = "Reorder quantity cannot be negative") private BigDecimal reorderQuantity;
    private Boolean isAvailable;
}
