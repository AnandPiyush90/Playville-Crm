package com.playville.crm.dto.staff;

import com.playville.crm.entity.enums.StaffRole;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateStaffRequest {

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @Email
    private String email;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Valid 10-digit mobile required")
    private String phone;

    @NotBlank
    @Size(min = 4, max = 60, message = "Username must be 4-60 characters")
    private String username;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private StaffRole role;
}