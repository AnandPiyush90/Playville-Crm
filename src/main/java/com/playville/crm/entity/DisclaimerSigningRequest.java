package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_disclaimer_signing_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DisclaimerSigningRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "onboarding_draft_id", nullable = false) private CustomerOnboardingDraft draft;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "template_id", nullable = false) private DisclaimerTemplate template;
    @Column(nullable = false) private String channel;
    @Column(nullable = false) @Builder.Default private String status = "CREATED";
    @Column(name = "idempotency_key", nullable = false) private String idempotencyKey;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "signed_at") private LocalDateTime signedAt;
    @Column(name = "token_sha256", unique = true, length = 64) private String tokenSha256;
    @Column(name = "sent_at") private LocalDateTime sentAt;
}
