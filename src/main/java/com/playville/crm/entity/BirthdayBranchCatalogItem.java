package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "pv_birthday_branch_catalog_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_birthday_branch_catalog", columnNames = {"branch_id", "catalog_item_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BirthdayBranchCatalogItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "catalog_item_id", nullable = false) private BirthdayCatalogItem catalogItem;
    @Column(name = "display_name_override", length = 150) private String displayNameOverride;
    @Column(name = "unit_price_override", precision = 12, scale = 2) private BigDecimal unitPriceOverride;
    @Column(name = "is_available", nullable = false) @Builder.Default private boolean available = true;
    @Column(name = "reserve_inventory", nullable = false) @Builder.Default private boolean reserveInventory = false;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
