package com.playville.crm.dto.catalogue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class UpdateProductRequest {
    @NotBlank @Size(max = 150) private String productName;
    private String description;
    @NotNull private Integer categoryId;
    private Boolean trackInventory;
    @Valid @NotEmpty private List<UpdateSkuRequest> skus;
}
