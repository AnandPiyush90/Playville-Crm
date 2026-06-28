package com.playville.crm.dto.schooltrip;

import com.playville.crm.entity.Purchase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter
public class SchoolTripRequest {

    @NotBlank(message = "School name is required")
    private String schoolName;

    private String contactPerson;
    private String contactPhone;
    private String contactEmail;

    @NotNull(message = "Trip date is required")
    private LocalDate tripDate;

    @NotNull(message = "Slot start is required")
    private LocalTime slotStart;

    @NotNull(message = "Slot end is required")
    private LocalTime slotEnd;

    private Integer    expectedKids;
    private BigDecimal pricePerKid;
    private BigDecimal totalAmount;
    private BigDecimal advancePaid;

    private Purchase.PaymentMode paymentMode;
    private String               paymentReference;
    private String               notes;
}