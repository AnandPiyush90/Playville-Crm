package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_tax_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "tax_code", nullable = false, length = 30) private String taxCode;
    @Column(name = "tax_name", nullable = false, length = 100) private String taxName;
    @Column(name = "hsn_sac_code", length = 30) private String hsnSacCode;
    @Column(name = "rate_percent", nullable = false, precision = 5, scale = 2) @Builder.Default private BigDecimal ratePercent = BigDecimal.ZERO;
    @Column(name = "price_includes_tax") @Builder.Default private boolean priceIncludesTax = true;
    @Column(name = "effective_from", nullable = false) private LocalDate effectiveFrom;
    @Column(name = "effective_to") private LocalDate effectiveTo;
    @Column(name = "is_active") @Builder.Default private boolean isActive = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
