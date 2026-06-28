package com.playville.crm.dto.kid;

import com.playville.crm.entity.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class CreateKidRequest {

    @NotBlank(message = "Kid name is required")
    @Size(max = 100)
    private String kidName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    private Gender gender;

    private String specialNotes;
}