package com.playville.crm.dto.customer;

import com.playville.crm.dto.kid.KidDto;
import com.playville.crm.entity.enums.LeadSource;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CustomerDto {
    private Integer       id;
    private String        phoneNumber;
    private String        parentName;
    private String        email;
    private LeadSource    leadSource;
    private Integer       globalSessionBalance;
    private String        currentPackageName;
    private Integer       homeBranchId;
    private String        homeBranchCode;
    private boolean       disclaimerAccepted;
    private Integer       totalVisits;
    private String        notes;
    private boolean       isActive;
    private List<KidDto>  kids;
    private LocalDateTime createdAt;
}