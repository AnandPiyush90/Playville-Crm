package com.playville.crm.dto.catalogue;

import com.playville.crm.entity.enums.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.util.List;

@Getter @Setter
public class CreateProductRequest {
    @NotBlank(message = "Product code is required") @Size(max = 60) private String productCode;
    @NotBlank(message = "Product name is required") @Size(max = 150) private String productName;
    private String description;
    @NotNull(message = "Category is required") private Integer categoryId;
    @NotNull(message = "Product type is required") private ProductType productType;
    private Integer packageId;
    private Boolean trackInventory;
    @Valid @NotEmpty(message = "At least one SKU is required") private List<SkuRequest> skus;
}
