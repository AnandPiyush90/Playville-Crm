package com.playville.crm.dto.invoice;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;
@Getter @Setter public class ReturnInvoiceLineRequest { @NotNull(message = "Invoice line ID is required") private Integer invoiceItemId; @NotNull(message = "Return quantity is required") @DecimalMin(value = "0.001", message = "Return quantity must be greater than zero") private BigDecimal quantity; private Boolean resellable; }
