package com.playville.crm.dto.notification;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder public class EmailTemplateResponse {
    private String key,name,description,subject,bodyText;
    private List<Variable> variables;
    private boolean customized;
    private Long version;
    private LocalDateTime updatedAt;
    @Getter @AllArgsConstructor public static class Variable { private String key,label,example; }
}
