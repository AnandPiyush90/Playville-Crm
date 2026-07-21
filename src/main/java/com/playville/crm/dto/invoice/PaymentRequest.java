package com.playville.crm.dto.invoice;

import com.playville.crm.entity.enums.InvoicePaymentMode;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;
@Getter @Setter public class PaymentRequest { @NotNull(message = "Payment mode is required") private InvoicePaymentMode paymentMode; @NotNull(message = "Payment amount is required") @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero") private BigDecimal amount; @Size(max = 100) private String providerReference; }
