package com.playville.crm.dto.birthday;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class BirthdayCatalogItemRequest {
    @NotBlank @Size(max = 80) @Pattern(regexp = "^[A-Z0-9][A-Z0-9_-]*$") private String itemCode;
    @NotBlank @Pattern(regexp = "FOOD|ADD_ON|EXTRA") private String category;
    private Integer skuId;
    @NotBlank @Size(max = 150) private String itemName;
    @Size(max = 500) private String description;
    @NotBlank @Size(max = 40) private String unitLabel;
    @NotNull @DecimalMin("0.0") private BigDecimal unitPrice;
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") private BigDecimal taxRate;
    @NotBlank @Pattern(regexp = "NONE|OPTIONAL|REQUIRED") private String inventoryMode;
    @NotNull @DecimalMin("0.001") private BigDecimal minimumOrderQuantity;
    @NotNull @Min(0) private Integer leadTimeDays;
    private Boolean active;
}
