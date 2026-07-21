package com.playville.crm.dto.staff;

import com.playville.crm.entity.enums.StaffRole;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateStaffRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be 100 characters or less")
    private String fullName;

    @Email(message = "Enter a valid email address")
    private String email;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String phone;

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 60, message = "Username must be 4-60 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private StaffRole role;
}
