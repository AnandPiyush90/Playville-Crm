package com.playville.crm.dto.report;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Builder
public class TrialFunnelResponse {
    private Integer branchId;
    private LocalDate from;
    private LocalDate to;
    private long issued;
    private long checkedIn;
    private long completed;
    private long purchasedSameDay;
    private long purchasedWithinSevenDays;
    private long expiredOrNoShow;
    private long declined;
    private BigDecimal conversionRate;
}
