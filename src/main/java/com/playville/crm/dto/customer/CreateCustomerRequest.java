package com.playville.crm.dto.customer;

import com.playville.crm.entity.enums.LeadSource;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateCustomerRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter valid 10-digit Indian mobile number")
    private String phoneNumber;

    @NotBlank(message = "Parent name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String parentName;

    @Email(message = "Enter a valid email address")
    private String email;

    private LeadSource leadSource;

    private Boolean disclaimerAccepted;
}