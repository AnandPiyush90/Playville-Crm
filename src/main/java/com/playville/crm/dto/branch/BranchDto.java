package com.playville.crm.dto.branch;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Builder
public class BranchDto {
    private Integer    id;
    private String     branchCode;
    private String     branchName;
    private String     address;
    private String     city;
    private String     phone;
    private LocalTime  openTime;
    private LocalTime  closeTime;
    private String     closedDay;
    private BigDecimal settlementRate;
    private String     notificationEmail;
    private boolean    isActive;
}
