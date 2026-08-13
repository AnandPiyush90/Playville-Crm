package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_customer_onboarding_drafts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerOnboardingDraft {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @Column(name = "parent_name", nullable = false) private String parentName;
    @Column(name = "phone_number", nullable = false) private String phoneNumber;
    private String email;
    @Column(name = "visit_purpose", nullable = false) private String visitPurpose;
    @Column(name = "children_json", columnDefinition = "json", nullable = false) private String childrenJson;
    @Column(nullable = false) @Builder.Default private String status = "DRAFT";
    @Column(name = "idempotency_key", nullable = false) private String idempotencyKey;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "completed_customer_id") private Customer completedCustomer;
}
