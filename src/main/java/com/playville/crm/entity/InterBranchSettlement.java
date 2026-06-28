package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_inter_branch_settlements")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterBranchSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_id", nullable = false)
    private Checkin checkin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_branch_id", nullable = false)
    private Branch purchaseBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usage_branch_id", nullable = false)
    private Branch usageBranch;

    @Column(name = "sessions_used")
    @Builder.Default
    private Integer sessionsUsed = 1;

    @Column(name = "settlement_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal settlementRate;

    @Column(name = "settlement_amount", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal settlementAmount = BigDecimal.ZERO;

    @Column(name = "settled")
    @Builder.Default
    private boolean settled = false;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settled_by_staff_id")
    private Staff settledByStaff;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}