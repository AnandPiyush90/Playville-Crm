package com.playville.crm.dto.notification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class EmailTestRequest { @NotBlank @Email private String destination; }
