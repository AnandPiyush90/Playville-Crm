package com.playville.crm.dto.customer;

import com.playville.crm.entity.enums.LeadSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCustomerRequest {

    @Size(max = 150, message = "Parent name must be 150 characters or less")
    private String     parentName;

    @Email(message = "Enter a valid email address")
    private String     email;

    private LeadSource leadSource;
    private String     notes;
    private Boolean    disclaimerAccepted;
}
