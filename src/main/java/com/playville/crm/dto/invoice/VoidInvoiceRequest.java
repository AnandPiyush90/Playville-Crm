package com.playville.crm.dto.invoice;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter; import lombok.Setter;
@Getter @Setter public class VoidInvoiceRequest { @NotBlank(message = "Void reason is required") private String reason; }
