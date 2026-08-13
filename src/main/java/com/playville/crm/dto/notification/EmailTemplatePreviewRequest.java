package com.playville.crm.dto.notification;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter @Setter public class EmailTemplatePreviewRequest { private Map<String,String> variables; }
