package com.playville.crm.dto.birthday;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class BirthdayBranchCatalogRequest {
    @Size(max = 150) private String displayNameOverride;
    @DecimalMin("0.0") private BigDecimal unitPriceOverride;
    @NotNull private Boolean available;
    @NotNull private Boolean reserveInventory;
}
