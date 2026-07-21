package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.playville.crm.entity.enums.PurchaseContext;

@Entity
@Table(name = "pv_purchases")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private PlayvillePackage playvillePackage;

    @Column(name = "sessions_added", nullable = false)
    private Integer sessionsAdded;

    @Column(name = "amount_paid", nullable = false, precision = 8, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "gst_amount", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "discount_applied", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal discountApplied = BigDecimal.ZERO;

    @Column(name = "balance_before", nullable = false)
    private Integer balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "is_upgrade")
    @Builder.Default
    private boolean isUpgrade = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upgraded_from_package")
    private PlayvillePackage upgradedFromPackage;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 20)
    @Builder.Default
    private PaymentMode paymentMode = PaymentMode.Cash;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "source_checkin_id")
    private Checkin sourceCheckin;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "source_trial_entitlement_id")
    private CustomerEntitlement sourceTrialEntitlement;

    @Enumerated(EnumType.STRING) @Column(name = "purchase_context", length = 30)
    @Builder.Default private PurchaseContext purchaseContext = PurchaseContext.STANDARD;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PaymentMode { Cash, UPI, Card, Online }
}
