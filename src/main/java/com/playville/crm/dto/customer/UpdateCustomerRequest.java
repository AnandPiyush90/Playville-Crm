package com.playville.crm.dto.customer;

import com.playville.crm.entity.enums.LeadSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCustomerRequest {

    @Size(max = 150)
    private String     parentName;

    @Email
    private String     email;

    private LeadSource leadSource;
    private String     notes;
    private Boolean    disclaimerAccepted;
}