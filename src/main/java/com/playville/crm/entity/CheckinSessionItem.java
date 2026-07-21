package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_checkin_session_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CheckinSessionItem {
    public enum Status { OPEN, CHARGED, CANCELLED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "checkin_id", nullable = false) private Checkin checkin;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sku_id", nullable = false) private ProductSku sku;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "added_by_staff_id") private Staff addedByStaff;
    @Column(name = "product_name_snapshot", nullable = false, length = 150) private String productNameSnapshot;
    @Column(name = "sku_code_snapshot", nullable = false, length = 80) private String skuCodeSnapshot;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal quantity;
    @Column(name = "unit_price_snapshot", nullable = false, precision = 10, scale = 2) private BigDecimal unitPriceSnapshot;
    @Column(name = "tax_rate_snapshot", nullable = false, precision = 5, scale = 2) private BigDecimal taxRateSnapshot;
    @Column(name = "taxable_amount", nullable = false, precision = 12, scale = 2) private BigDecimal taxableAmount;
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2) private BigDecimal taxAmount;
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2) private BigDecimal lineTotal;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) @Builder.Default private Status status = Status.OPEN;
    @Column(length = 250) private String notes;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
