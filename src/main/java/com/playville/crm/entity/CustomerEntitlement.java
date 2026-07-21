package com.playville.crm.entity;

import com.playville.crm.entity.enums.EntitlementStatus;
import com.playville.crm.entity.enums.EntitlementType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_customer_entitlements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerEntitlement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", nullable = false) private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private EntitlementType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EntitlementStatus status;
    @Column(name = "sessions_granted", nullable = false) private Integer sessionsGranted;
    @Column(name = "sessions_reserved", nullable = false) @Builder.Default private Integer sessionsReserved = 0;
    @Column(name = "campaign_code", length = 60) private String campaignCode;
    @Column(name = "reason_text", columnDefinition = "TEXT") private String reasonText;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Version private Long version;
    @Column(name = "created_at", updatable = false) @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at") @Builder.Default private LocalDateTime updatedAt = LocalDateTime.now();
}
