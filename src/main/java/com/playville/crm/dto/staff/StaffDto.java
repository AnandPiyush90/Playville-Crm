package com.playville.crm.dto.staff;

import com.playville.crm.entity.enums.StaffRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StaffDto {
    private Integer       id;
    private Integer       branchId;
    private String        branchCode;
    private String        fullName;
    private String        email;
    private String        phone;
    private String        username;
    private StaffRole     role;
    private boolean       isActive;
    private LocalDateTime createdAt;
}