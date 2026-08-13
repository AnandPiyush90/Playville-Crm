package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_disclaimer_acceptances")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DisclaimerAcceptance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id") private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id") private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "onboarding_draft_id") private CustomerOnboardingDraft draft;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "signing_request_id") private DisclaimerSigningRequest request;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "template_id") private DisclaimerTemplate template;
    private String templateCodeSnapshot, templateVersionSnapshot, templateTitleSnapshot, contentSha256;
    private String acceptanceMethod, signerName, signerRelationship, signerEmail, signerPhone;
    private String signatureMimeType, signatureSha256, evidenceSha256;
    @Column(name = "accepted_at") private LocalDateTime acceptedAt;
    @Lob @Column(name = "signature_image") private byte[] signatureImage;
    @Column(nullable = false) @Builder.Default private String status = "VALID";
}
