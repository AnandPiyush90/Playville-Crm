package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "pv_birthday_packages")
@Getter @Setter
public class BirthdayPackage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "package_code", nullable = false, unique = true) private String packageCode;
    @Column(name = "package_name", nullable = false) private String packageName;
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2) private BigDecimal basePrice;
    @Column(name = "included_kids", nullable = false) private Integer includedKids;
    @Column(name = "included_adults", nullable = false) private Integer includedAdults;
    @Column(name = "extra_kid_price", nullable = false, precision = 12, scale = 2) private BigDecimal extraKidPrice;
    @Column(name = "extra_adult_price", nullable = false, precision = 12, scale = 2) private BigDecimal extraAdultPrice;
    @Column(name = "included_duration_minutes", nullable = false) private Integer includedDurationMinutes;
    @Column(nullable = false) private boolean active;
}
