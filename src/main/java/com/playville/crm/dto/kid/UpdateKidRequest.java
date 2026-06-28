package com.playville.crm.dto.kid;

import java.time.LocalDate;

import com.playville.crm.entity.enums.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record UpdateKidRequest(
        @Size(max = 100) String kidName,
        @Past LocalDate dob,
        Gender gender,
        String specialNotes,
        Boolean active) {
}
