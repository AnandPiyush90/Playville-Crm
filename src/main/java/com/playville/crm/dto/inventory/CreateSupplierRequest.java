package com.playville.crm.dto.inventory;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
@Getter @Setter public class CreateSupplierRequest { @NotBlank(message = "Supplier name is required") @Size(max = 150) private String supplierName; @Size(max = 20) private String phone; @Email(message = "Enter a valid email address") @Size(max = 150) private String email; @Size(max = 50) private String taxNumber; }
