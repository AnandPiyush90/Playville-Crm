package com.playville.crm.dto.packages;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class PackageRequest {

    @NotBlank(message = "Package name is required")
    @Size(max = 100)
    private String packageName;

    @NotNull(message = "Sessions purchased is required")
    @Positive(message = "Sessions purchased must be positive")
    private Integer sessionsPurchased;

    @Min(value = 0, message = "Bonus sessions cannot be negative")
    private Integer sessionsBonus;

    @NotNull(message = "Price per session is required")
    @Positive
    private BigDecimal pricePerSession;

    @NotNull(message = "Total price is required")
    @Positive
    private BigDecimal totalPrice;

    private Integer    validityDays;

    @DecimalMin("0.0")
    private BigDecimal birthdayDiscountPct;

    @DecimalMin("0.0")
    private BigDecimal rechargeDiscountPct;

    private Boolean    canUpgrade;
    private Integer    displayOrder;
}