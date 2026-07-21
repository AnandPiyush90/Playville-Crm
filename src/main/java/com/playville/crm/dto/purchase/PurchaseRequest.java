package com.playville.crm.dto.purchase;

import com.playville.crm.entity.Purchase;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class PurchaseRequest {

    @NotNull(message = "Customer ID is required")
    private Integer customerId;

    @NotNull(message = "Package ID is required")
    private Integer packageId;

    @NotNull(message = "Payment mode is required")
    private Purchase.PaymentMode paymentMode;

    private String     paymentReference;

    @Positive(message = "Discount must be positive")
    private BigDecimal discountApplied;

    private String notes;
    private Integer sourceCheckinId;
    private Integer sourceTrialEntitlementId;
    private com.playville.crm.entity.enums.PurchaseContext purchaseContext;
}
