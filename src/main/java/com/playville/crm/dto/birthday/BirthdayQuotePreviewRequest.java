package com.playville.crm.dto.birthday;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;

@Getter @Setter
public class BirthdayQuotePreviewRequest {
    @NotNull private Integer packageId;
    private LocalDate partyDate;
    @NotNull @Min(0) private Integer kidsCount;
    @NotNull @Min(0) private Integer adultsCount;
    @Valid private FoodBoxRequest kidsFoodBox;
    @Valid private FoodBoxRequest adultsFoodBox;
    @Valid private List<CatalogSelectionRequest> addOns;
    private Integer decorationPackageId;
    @Valid private List<CatalogSelectionRequest> extras;

    @Getter @Setter
    public static class FoodBoxRequest {
        @NotNull @Min(0) private Integer boxCount;
        @Valid private List<FoodComponentRequest> components;
    }

    @Getter @Setter
    public static class FoodComponentRequest {
        @NotNull private Integer catalogItemId;
        @NotNull @Positive private BigDecimal quantityPerBox;
    }

    @Getter @Setter
    public static class CatalogSelectionRequest {
        @NotNull private Integer catalogItemId;
        @NotNull @Positive private BigDecimal quantity;
    }
}
