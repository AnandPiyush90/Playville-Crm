package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "pv_onboarding_idempotency")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OnboardingIdempotency {
    @Id @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id") private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "entitlement_id") private CustomerEntitlement entitlement;
    @Column(name = "next_action", length = 30) private String nextAction;
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "COMPLETED";
    @Column(name = "created_at", nullable = false) @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
