package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "pv_notification_deliveries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationDelivery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invoice_id") private Invoice invoice;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id") private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "triggered_by_staff_id") private Staff triggeredByStaff;
    @Column(nullable = false, length = 20) private String channel;
    @Column(nullable = false, length = 150) private String destination;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "provider_reference", length = 150) private String providerReference;
    @Column(name = "error_message", columnDefinition = "TEXT") private String errorMessage;
    @Column(name = "idempotency_key", nullable = false, length = 100) private String idempotencyKey;
    @Column(nullable = false, length = 40) @Builder.Default private String purpose = "INVOICE";
    @Column(name = "reference_type", length = 40) private String referenceType;
    @Column(name = "reference_id", length = 80) private String referenceId;
    @Column(name = "provider_type", length = 30) private String providerType;
    @Column(name = "attempt_count", nullable = false) @Builder.Default private int attemptCount = 0;
    @Column(name = "next_retry_at") private LocalDateTime nextRetryAt;
    @Column(name = "last_attempt_at") private LocalDateTime lastAttemptAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "sent_at") private LocalDateTime sentAt;
}
