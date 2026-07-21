package com.playville.crm.dto.staff;

import com.playville.crm.entity.enums.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateStaffRequest(
        Integer branchId,
        @Size(max = 100, message = "Full name must be 100 characters or less") String fullName,
        @Email(message = "Enter a valid email address") @Size(max = 150, message = "Email must be 150 characters or less") String email,
        @Size(max = 15) String phone,
        @Size(max = 100) String password,
        StaffRole role,
        Boolean active) {
}
