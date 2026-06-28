package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_packages")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayvillePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "package_name", nullable = false, length = 100)
    private String packageName;

    @Column(name = "sessions_purchased", nullable = false)
    private Integer sessionsPurchased;

    @Column(name = "sessions_bonus")
    @Builder.Default
    private Integer sessionsBonus = 0;

    @Column(name = "sessions_total", insertable = false, updatable = false)
    private Integer sessionsTotal;

    @Column(name = "price_per_session", nullable = false, precision = 8, scale = 2)
    private BigDecimal pricePerSession;

    @Column(name = "total_price", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(name = "birthday_discount_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal birthdayDiscountPct = BigDecimal.ZERO;

    @Column(name = "recharge_discount_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal rechargeDiscountPct = BigDecimal.ZERO;

    @Column(name = "can_upgrade")
    @Builder.Default
    private boolean canUpgrade = true;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}