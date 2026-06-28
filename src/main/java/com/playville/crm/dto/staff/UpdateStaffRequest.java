package com.playville.crm.dto.staff;

import com.playville.crm.entity.enums.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateStaffRequest(
        Integer branchId,
        @Size(max = 100) String fullName,
        @Email @Size(max = 150) String email,
        @Size(max = 15) String phone,
        @Size(max = 100) String password,
        StaffRole role,
        Boolean active) {
}
