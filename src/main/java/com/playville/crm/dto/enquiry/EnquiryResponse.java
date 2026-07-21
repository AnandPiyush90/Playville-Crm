package com.playville.crm.dto.enquiry;
import com.playville.crm.entity.enums.*;
import lombok.*;
import java.time.*;
@Getter @Builder public class EnquiryResponse { private Integer id; private Integer branchId; private Integer convertedCustomerId; private String parentName; private String phoneNumber; private String email; private LeadSource leadSource; private EnquiryStatus status; private LocalDateTime visitScheduledAt; private String childName; private LocalDate childDob; private String notes; private LocalDateTime createdAt; private LocalDateTime updatedAt; }
