package com.playville.crm.dto.catalogue;

import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;

@Getter @Setter
public class CreateCategoryRequest {
    @NotBlank(message = "Category code is required") @Size(max = 50) private String categoryCode;
    @NotBlank(message = "Category name is required") @Size(max = 100) private String categoryName;
    private Integer parentId;
    @Min(value = 0, message = "Display order cannot be negative") private Integer displayOrder;
}
