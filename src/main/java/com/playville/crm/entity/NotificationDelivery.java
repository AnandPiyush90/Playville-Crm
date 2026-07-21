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
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "sent_at") private LocalDateTime sentAt;
}
