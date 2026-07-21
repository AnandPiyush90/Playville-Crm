package com.playville.crm.entity;

import com.playville.crm.entity.enums.EntitlementTransactionType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_entitlement_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EntitlementTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "entitlement_id", nullable = false)
    private CustomerEntitlement entitlement;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "checkin_id")
    private Checkin checkin;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "staff_id")
    private Staff staff;
    @Enumerated(EnumType.STRING) @Column(name = "transaction_type", nullable = false, length = 30)
    private EntitlementTransactionType transactionType;
    @Column(name = "sessions_delta", nullable = false)
    private Integer sessionsDelta;
    @Column(name = "reserved_delta", nullable = false)
    private Integer reservedDelta;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(name = "occurred_at", nullable = false)
    @Builder.Default private LocalDateTime occurredAt = LocalDateTime.now();
}
