package com.playville.crm.dto.birthday;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;

@Getter @Builder
public class BirthdayQuotePreviewResponse {
    private Integer packageId;
    private String packageName;
    private Integer includedKids;
    private Integer includedAdults;
    private Integer extraKids;
    private Integer extraAdults;
    private List<QuoteLine> lines;
    private BigDecimal subtotal;
    private BigDecimal taxTotal;
    private BigDecimal grandTotal;

    @Getter @Builder
    public static class QuoteLine {
        private Integer catalogItemId;
        private Integer skuId;
        private boolean inventoryReservedOnConfirmation;
        private String category;
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private BigDecimal lineTotal;
    }
}
