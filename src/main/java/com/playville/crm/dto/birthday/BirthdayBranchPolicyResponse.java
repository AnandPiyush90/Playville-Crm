package com.playville.crm.dto.birthday;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
@Getter @Builder
public class BirthdayBranchPolicyResponse {
    private Integer branchId;
    private BigDecimal extraKidPrice;
    private BigDecimal extraAdultPrice;
    private BigDecimal extraTime30MinPrice;
    private Boolean enquiryCalendarEnabled;
    private String cancellationPolicyJson;
    private String refundPolicyJson;
    private String shareChannel;
    private String shareSettingsJson;
}
