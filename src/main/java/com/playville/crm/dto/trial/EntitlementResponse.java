package com.playville.crm.dto.trial;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EntitlementResponse {
    private Integer id;
    private String type;
    private String status;
    private Integer sessionsGranted;
    private Integer sessionsReserved;
    private String campaignCode;
    private LocalDateTime expiresAt;
}
