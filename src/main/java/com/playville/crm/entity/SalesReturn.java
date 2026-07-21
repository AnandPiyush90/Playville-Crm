package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "pv_sales_returns") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalesReturn {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "original_invoice_id", nullable = false) private Invoice originalInvoice;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "credit_invoice_id", nullable = false) private Invoice creditInvoice;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @Column(nullable = false, length = 255) private String reason;
    @Column(columnDefinition = "TEXT") private String notes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "returned_by_staff_id") private Staff returnedByStaff;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
