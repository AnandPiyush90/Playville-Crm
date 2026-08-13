package com.playville.crm.dto.notification;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @Builder public class NotificationDeliveryResponse {
    private Integer id; private String purpose,channel,destination,status,referenceType,referenceId,errorCode;
    private int attemptCount; private LocalDateTime createdAt,sentAt,nextRetryAt,lastAttemptAt;
}
