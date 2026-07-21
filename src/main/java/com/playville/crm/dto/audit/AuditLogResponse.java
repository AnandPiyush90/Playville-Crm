package com.playville.crm.dto.audit;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @Builder
public class AuditLogResponse {
    private Long id;
    private Integer branchId;
    private String branchCode;
    private Integer actorStaffId;
    private String actorUsername;
    private String actorName;
    private String actorRole;
    private String actionType;
    private String resourceType;
    private String resourceId;
    private String outcome;
    private String description;
    private String httpMethod;
    private String requestPath;
    private Integer responseStatus;
    private String ipAddress;
    private String correlationId;
    private String metadataJson;
    private LocalDateTime occurredAt;
}
