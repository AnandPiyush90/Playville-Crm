package com.playville.crm.dto.disclaimer;
import lombok.*;
import java.time.*;
@Getter @Setter @Builder
public class DisclaimerView {
     private Long id;
     private Long acceptanceId;
     private Integer customerId;
     private String status,templateCode,version,title,contentHtml,signerName,signerRelationship,evidenceSha256,channel,email;
     private String customerName,phone,acceptanceMethod,signatureDataUrl,childrenSummary;
     private Boolean hasSignature;
     private LocalDateTime expiresAt,acceptedAt,sentAt;
    }
