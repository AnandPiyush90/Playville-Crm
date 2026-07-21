package com.playville.crm.dto.checkin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class CheckinRequest {

    @NotNull(message = "Customer ID is required")
    private Integer customerId;

    @NotEmpty(message = "At least one kid must be selected for check-in")
    private List<Integer> kidIds;

    private String visitType;
    private Integer entitlementId;
    private String notes;
}
