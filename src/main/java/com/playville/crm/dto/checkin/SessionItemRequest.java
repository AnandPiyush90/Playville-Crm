package com.playville.crm.dto.checkin;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class SessionItemRequest {
    @NotNull private Integer skuId;
    @NotNull @DecimalMin(value = "0.001") private BigDecimal quantity;
    @Size(max = 250) private String notes;
}
