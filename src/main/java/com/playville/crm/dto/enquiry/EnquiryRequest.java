package com.playville.crm.dto.enquiry;
import com.playville.crm.entity.enums.LeadSource;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Getter @Setter public class EnquiryRequest { @NotBlank(message = "Parent name is required") private String parentName; @NotBlank(message = "Phone number is required") @Pattern(regexp = "^[0-9+() -]{10,20}$", message = "Enter a valid phone number") private String phoneNumber; @Email(message = "Enter a valid email address") private String email; private LeadSource leadSource; private LocalDateTime visitScheduledAt; @Size(max = 100) private String childName; @Past(message = "Child date of birth must be in the past") private LocalDate childDob; private String notes; }
