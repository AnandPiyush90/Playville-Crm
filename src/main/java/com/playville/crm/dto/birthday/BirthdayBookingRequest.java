package com.playville.crm.dto.birthday;

import com.playville.crm.entity.Purchase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter
public class BirthdayBookingRequest {

    @NotNull(message = "Customer ID is required")
    private Integer customerId;

    @NotNull(message = "Kid ID is required")
    private Integer kidId;

    @NotNull(message = "Party date is required")
    private LocalDate partyDate;

    @NotNull(message = "Slot start time is required")
    private LocalTime partySlotStart;

    private Integer    expectedGuests;
    private String     cakeOption;
    private Integer    foodBoxesCount;

    @Positive
    private BigDecimal baseAmount;

    private BigDecimal discountPct;
    private BigDecimal advancePaid;

    private Purchase.PaymentMode paymentMode;
    private String               paymentReference;
    private String               notes;

    @Valid private BirthdayQuotePreviewRequest quote;
}