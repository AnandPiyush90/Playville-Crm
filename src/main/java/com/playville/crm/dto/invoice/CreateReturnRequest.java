package com.playville.crm.dto.invoice;
import com.playville.crm.entity.enums.InvoicePaymentMode;
import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.util.List;
@Getter @Setter public class CreateReturnRequest { @NotBlank(message = "Return reason is required") @Size(max = 255) private String reason; private String notes; @NotNull(message = "Refund mode is required") private InvoicePaymentMode refundMode; @Valid @NotEmpty(message = "At least one return line is required") private List<ReturnInvoiceLineRequest> items; }
