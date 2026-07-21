package com.playville.crm.dto.inventory;
import lombok.Builder; import lombok.Getter;
@Getter @Builder public class SupplierResponse { private Integer id; private String supplierName; private String phone; private String email; private String taxNumber; private boolean active; }
