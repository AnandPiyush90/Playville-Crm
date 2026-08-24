package com.playville.crm.dto.notification;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class EmailTestRequest {
    @NotBlank(message = "Destination can't be empty")
    @Email
    @JsonAlias("recipient")
    private String destination;
}
