package com.playville.crm.dto.catalogue;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class UpdateSkuRequest {
    @NotNull private Integer id;
    @NotBlank @Size(max = 80) private String skuCode;
    @Size(max = 100) private String barcode;
    private String variantAttributesJson;
    @NotBlank @Size(max = 20) private String unitOfMeasure;
    @NotNull @DecimalMin("0.0") private BigDecimal defaultSalePrice;
    @DecimalMin("0.0") private BigDecimal defaultCostPrice;
    private Integer taxProfileId;
    private Boolean hasExpiry;
    private Boolean active;
}
