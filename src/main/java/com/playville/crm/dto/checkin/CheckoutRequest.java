package com.playville.crm.dto.checkin;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.playville.crm.entity.enums.*;

@Getter @Setter
public class CheckoutRequest {

    @DecimalMin(value = "0.0", message = "Extra charges cannot be negative")
    private BigDecimal extraCharges;

    private String checkoutNotes;
    private ConversionOutcome conversionOutcome;
    private ConversionReason conversionReason;
    private Integer purchaseId;
    private LocalDateTime followUpAt;

    // GST calculated server-side at 18% of extraCharges
}
