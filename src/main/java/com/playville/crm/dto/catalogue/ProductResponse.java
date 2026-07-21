package com.playville.crm.dto.catalogue;

import com.playville.crm.entity.enums.ProductType;
import lombok.Builder; import lombok.Getter;
import java.util.List;
@Getter @Builder public class ProductResponse { private Integer id; private String productCode; private String productName; private String description; private Integer categoryId; private String categoryName; private ProductType productType; private Integer packageId; private boolean trackInventory; private boolean active; private List<SkuResponse> skus; }
