package com.playville.crm.dto.invoice;

import com.playville.crm.entity.enums.InvoicePaymentMode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {
    private Integer id;
    private String paymentType;
    private InvoicePaymentMode paymentMode;
    private BigDecimal amount;
    private String providerReference;
    private String status;
    private LocalDateTime paidAt;
}
