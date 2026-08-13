package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_email_provider_configs", uniqueConstraints = @UniqueConstraint(name = "uq_email_provider_branch", columnNames = "branch_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailProviderConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @Column(name = "provider_type", nullable = false, length = 30) private String providerType;
    @Column(nullable = false, length = 150) private String host;
    @Column(nullable = false) private int port;
    @Column(nullable = false, length = 150) private String username;
    @Column(name = "encrypted_password", nullable = false, columnDefinition = "TEXT") private String encryptedPassword;
    @Column(name = "tls_mode", nullable = false, length = 20) private String tlsMode;
    @Column(name = "from_email", nullable = false, length = 150) private String fromEmail;
    @Column(name = "from_name", nullable = false, length = 150) private String fromName;
    @Column(name = "reply_to_email", length = 150) private String replyToEmail;
    @Column(nullable = false) @Builder.Default private boolean enabled = false;
    @Column(name = "last_test_status", length = 20) private String lastTestStatus;
    @Column(name = "last_tested_at") private LocalDateTime lastTestedAt;
    @Column(name = "last_error_code", length = 100) private String lastErrorCode;
}
