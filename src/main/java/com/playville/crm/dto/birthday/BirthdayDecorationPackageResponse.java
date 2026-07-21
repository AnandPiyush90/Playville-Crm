package com.playville.crm.dto.birthday;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
@Getter @Builder
public class BirthdayDecorationPackageResponse {
    private Integer id;
    private String packageCode;
    private String packageName;
    private String description;
    private BigDecimal price;
    private BigDecimal taxRate;
    private Boolean active;
}
