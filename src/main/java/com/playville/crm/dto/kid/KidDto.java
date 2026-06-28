package com.playville.crm.dto.kid;

import com.playville.crm.entity.enums.Gender;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class KidDto {
    private Integer       id;
    private String        kidName;
    private LocalDate     dob;
    private int           ageInYears;
    private Gender        gender;
    private String        specialNotes;
    private boolean       isActive;
    private boolean       eligible;
    private LocalDateTime createdAt;
}