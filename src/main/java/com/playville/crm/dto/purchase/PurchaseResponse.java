package com.playville.crm.dto.purchase;

import com.playville.crm.entity.Purchase;
import com.playville.crm.entity.enums.PurchaseContext;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class PurchaseResponse {
    private Integer              id;
    private Integer              invoiceId;
    private String               invoiceNumber;
    private Integer              customerId;
    private String               parentName;
    private String               phoneNumber;
    private Integer              packageId;
    private String               packageName;
    private Integer              sessionsAdded;
    private BigDecimal           amountPaid;
    private BigDecimal           gstAmount;
    private BigDecimal           discountApplied;
    private Integer              balanceBefore;
    private Integer              balanceAfter;
    private boolean              isUpgrade;
    private Purchase.PaymentMode paymentMode;
    private String               paymentReference;
    private Integer              sourceCheckinId;
    private Integer              sourceTrialEntitlementId;
    private PurchaseContext      purchaseContext;
    private LocalDateTime        createdAt;
}
