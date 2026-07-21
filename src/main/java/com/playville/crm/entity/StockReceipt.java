package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "pv_stock_receipts") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockReceipt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(name = "supplier_invoice_reference", length = 100) private String supplierInvoiceReference;
    @Column(name = "received_at", nullable = false) private LocalDateTime receivedAt;
    @Column(columnDefinition = "TEXT") private String notes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "received_by_staff_id") private Staff receivedByStaff;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
