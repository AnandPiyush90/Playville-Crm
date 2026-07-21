package com.playville.crm.dto.invoice;

import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.util.List;
@Getter @Setter public class FinalizeInvoiceRequest { @NotNull(message = "Invoice version is required") private Long version; @Valid private List<PaymentRequest> payments; private Boolean completeCheckout; private String checkoutNotes; }
