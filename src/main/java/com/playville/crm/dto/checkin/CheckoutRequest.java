package com.playville.crm.dto.checkin;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class CheckoutRequest {

    @DecimalMin(value = "0.0", message = "Extra charges cannot be negative")
    private BigDecimal extraCharges;

    private String checkoutNotes;

    // GST calculated server-side at 18% of extraCharges
}