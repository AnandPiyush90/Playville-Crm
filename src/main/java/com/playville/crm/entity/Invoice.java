package com.playville.crm.entity;

import com.playville.crm.entity.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pv_invoices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "invoice_number", unique = true, length = 50) private String invoiceNumber;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id") private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "checkin_id") private Checkin checkin;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "birthday_booking_id") private BirthdayBooking birthdayBooking;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) @Builder.Default private InvoiceStatus status = InvoiceStatus.DRAFT;
    @Column(name = "invoice_type", nullable = false, length = 30) @Builder.Default private String invoiceType = "SALE";
    @Column(name = "invoice_date") private LocalDateTime invoiceDate;
    @Column(name = "seller_legal_name_snapshot", length = 150) private String sellerLegalNameSnapshot;
    @Column(name = "seller_address_snapshot", columnDefinition = "TEXT") private String sellerAddressSnapshot;
    @Column(name = "seller_phone_snapshot", length = 20) private String sellerPhoneSnapshot;
    @Column(name = "seller_email_snapshot", length = 150) private String sellerEmailSnapshot;
    @Column(name = "seller_gstin_snapshot", length = 15) private String sellerGstinSnapshot;
    @Column(name = "seller_pan_snapshot", length = 10) private String sellerPanSnapshot;
    @Column(name = "seller_tax_state_snapshot", length = 60) private String sellerTaxStateSnapshot;
    @Column(name = "seller_tax_state_code_snapshot", length = 2) private String sellerTaxStateCodeSnapshot;
    @Column(name = "terms_snapshot", columnDefinition = "TEXT") private String termsSnapshot;
    @Column(name = "footer_snapshot", length = 500) private String footerSnapshot;
    @Column(name = "customer_name_snapshot", length = 150) private String customerNameSnapshot;
    @Column(name = "customer_phone_snapshot", length = 15) private String customerPhoneSnapshot;
    @Column(name = "customer_email_snapshot", length = 150) private String customerEmailSnapshot;
    @Column(nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name = "discount_total", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal discountTotal = BigDecimal.ZERO;
    @Column(name = "tax_total", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal taxTotal = BigDecimal.ZERO;
    @Column(name = "rounding_adjustment", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal roundingAdjustment = BigDecimal.ZERO;
    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal grandTotal = BigDecimal.ZERO;
    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal amountPaid = BigDecimal.ZERO;
    @Column(name = "balance_due", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal balanceDue = BigDecimal.ZERO;
    @Column(columnDefinition = "TEXT") private String notes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "issued_by_staff_id") private Staff issuedByStaff;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "voided_by_staff_id") private Staff voidedByStaff;
    @Column(name = "void_reason", columnDefinition = "TEXT") private String voidReason;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @Version private Long version;
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true) @Builder.Default private List<InvoiceItem> items = new ArrayList<>();
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true) @Builder.Default private List<InvoicePayment> payments = new ArrayList<>();
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
