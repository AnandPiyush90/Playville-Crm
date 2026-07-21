package com.playville.crm.dto.catalogue;

import lombok.Builder; import lombok.Getter;
@Getter @Builder public class CategoryResponse { private Integer id; private Integer parentId; private String categoryCode; private String categoryName; private Integer displayOrder; private boolean active; }
