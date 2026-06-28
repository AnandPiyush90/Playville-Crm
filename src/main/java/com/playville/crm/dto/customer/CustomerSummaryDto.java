package com.playville.crm.dto.customer;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerSummaryDto {
    private Integer id;
    private String  phoneNumber;
    private String  parentName;
    private Integer globalSessionBalance;
    private String  currentPackageName;
    private Integer totalVisits;
    private boolean disclaimerAccepted;
}