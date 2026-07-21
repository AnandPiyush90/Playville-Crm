package com.playville.crm.entity;

import com.playville.crm.entity.enums.InvoicePaymentMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoicePayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invoice_id", nullable = false) private Invoice invoice;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @Column(name = "payment_type", nullable = false, length = 20) @Builder.Default private String paymentType = "PAYMENT";
    @Enumerated(EnumType.STRING) @Column(name = "payment_mode", nullable = false, length = 20) private InvoicePaymentMode paymentMode;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(name = "provider_reference", length = 100) private String providerReference;
    @Column(nullable = false, length = 20) @Builder.Default private String status = "COMPLETED";
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "received_by_staff_id") private Staff receivedByStaff;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @Column(name = "paid_at") private LocalDateTime paidAt;
    @Column(columnDefinition = "TEXT") private String notes;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
