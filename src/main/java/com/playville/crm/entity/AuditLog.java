package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_audit_logs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id") private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_staff_id") private Staff actorStaff;
    @Column(name = "actor_username", length = 60) private String actorUsername;
    @Column(name = "actor_name", length = 100) private String actorName;
    @Column(name = "actor_role", length = 20) private String actorRole;
    @Column(name = "action_type", nullable = false, length = 40) private String actionType;
    @Column(name = "resource_type", nullable = false, length = 60) private String resourceType;
    @Column(name = "resource_id", length = 80) private String resourceId;
    @Column(nullable = false, length = 20) private String outcome;
    @Column(nullable = false, length = 500) private String description;
    @Column(name = "http_method", length = 10) private String httpMethod;
    @Column(name = "request_path", length = 500) private String requestPath;
    @Column(name = "response_status") private Integer responseStatus;
    @Column(name = "ip_address", length = 64) private String ipAddress;
    @Column(name = "user_agent", length = 500) private String userAgent;
    @Column(name = "correlation_id", nullable = false, unique = true, length = 100) private String correlationId;
    @Column(name = "metadata_json", columnDefinition = "json") private String metadataJson;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
}
