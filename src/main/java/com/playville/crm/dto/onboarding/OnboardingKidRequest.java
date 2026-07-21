package com.playville.crm.dto.onboarding;
import com.playville.crm.entity.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.time.LocalDate;
@Getter @Setter public class OnboardingKidRequest { @NotBlank(message = "Kid name is required") private String kidName; @NotNull(message = "Date of birth is required") private LocalDate dob; private Gender gender; private String specialNotes; }
