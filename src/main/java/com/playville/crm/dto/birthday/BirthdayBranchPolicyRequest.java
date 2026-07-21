package com.playville.crm.dto.birthday;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Getter @Setter
public class BirthdayBranchPolicyRequest {
    @DecimalMin("0.00") private BigDecimal extraKidPrice;
    @DecimalMin("0.00") private BigDecimal extraAdultPrice;
    @DecimalMin("0.00") private BigDecimal extraTime30MinPrice;
    private Boolean enquiryCalendarEnabled;
    @Size(max = 10000) private String cancellationPolicyJson;
    @Size(max = 10000) private String refundPolicyJson;
    @Size(max = 30) private String shareChannel;
    @Size(max = 10000) private String shareSettingsJson;
}
