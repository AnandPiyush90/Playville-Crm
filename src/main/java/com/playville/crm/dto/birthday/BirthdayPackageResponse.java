package com.playville.crm.dto.birthday;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter @Builder
public class BirthdayPackageResponse {
    private Integer id;
    private String packageCode;
    private String packageName;
    private BigDecimal basePrice;
    private Integer includedKids;
    private Integer includedAdults;
    private BigDecimal extraKidPrice;
    private BigDecimal extraAdultPrice;
    private Integer includedDurationMinutes;
}
