package com.playville.crm.dto.enquiry;
import com.playville.crm.entity.enums.EnquiryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter; import lombok.Setter;
@Getter @Setter public class EnquiryStatusRequest { @NotNull private EnquiryStatus status; }
