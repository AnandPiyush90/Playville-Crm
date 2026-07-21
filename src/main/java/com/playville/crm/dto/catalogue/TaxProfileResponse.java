package com.playville.crm.dto.catalogue;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter @Builder
public class TaxProfileResponse {
    private Integer id;
    private String taxCode;
    private String taxName;
    private String hsnSacCode;
    private BigDecimal ratePercent;
    private boolean priceIncludesTax;
}
