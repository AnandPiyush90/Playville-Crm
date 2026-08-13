package com.playville.crm.dto.disclaimer;
import lombok.*;
import java.time.*;
@Getter @Setter @Builder
public class DisclaimerView {
     private Long id;
     private Long acceptanceId;
     private String status,templateCode,version,title,contentHtml,signerName,signerRelationship,evidenceSha256,channel,email;
     private LocalDateTime expiresAt,acceptedAt,sentAt;
    }
