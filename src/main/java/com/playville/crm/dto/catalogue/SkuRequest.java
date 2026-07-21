package com.playville.crm.dto.catalogue;

import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class SkuRequest {
    @NotBlank(message = "SKU code is required") @Size(max = 80) private String skuCode;
    @Size(max = 100) private String barcode;
    private String variantAttributesJson;
    @NotBlank(message = "Unit of measure is required") @Size(max = 20) private String unitOfMeasure;
    @NotNull(message = "Sale price is required") @DecimalMin(value = "0.0", inclusive = true, message = "Sale price cannot be negative") private BigDecimal defaultSalePrice;
    @DecimalMin(value = "0.0", inclusive = true, message = "Cost price cannot be negative") private BigDecimal defaultCostPrice;
    private Integer taxProfileId;
    private Boolean hasExpiry;
}
