package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_branch_skus", uniqueConstraints = @UniqueConstraint(name = "uq_branch_sku", columnNames = {"branch_id", "sku_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BranchSku {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sku_id", nullable = false) private ProductSku sku;
    @Column(name = "sale_price_override", precision = 10, scale = 2) private BigDecimal salePriceOverride;
    @Column(name = "reorder_level", nullable = false, precision = 12, scale = 3) @Builder.Default private BigDecimal reorderLevel = BigDecimal.ZERO;
    @Column(name = "reorder_quantity", nullable = false, precision = 12, scale = 3) @Builder.Default private BigDecimal reorderQuantity = BigDecimal.ZERO;
    @Column(name = "allow_negative_stock") @Builder.Default private boolean allowNegativeStock = false;
    @Column(name = "is_available") @Builder.Default private boolean isAvailable = true;
    @Version private Long version;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
