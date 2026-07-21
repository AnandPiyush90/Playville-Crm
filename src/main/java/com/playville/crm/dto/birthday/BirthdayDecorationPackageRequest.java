package com.playville.crm.dto.birthday;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Getter @Setter
public class BirthdayDecorationPackageRequest {
    @NotBlank @Size(max = 80) private String packageCode;
    @NotBlank @Size(max = 150) private String packageName;
    @Size(max = 5000) private String description;
    @NotNull @DecimalMin("0.00") private BigDecimal price;
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") private BigDecimal taxRate;
    private Boolean active;
}
