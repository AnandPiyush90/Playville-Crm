package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_inventory_balances", uniqueConstraints = @UniqueConstraint(name = "uq_inventory_balance", columnNames = {"branch_id", "sku_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryBalance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sku_id", nullable = false) private ProductSku sku;
    @Column(name = "quantity_on_hand", nullable = false, precision = 12, scale = 3) @Builder.Default private BigDecimal quantityOnHand = BigDecimal.ZERO;
    @Column(name = "quantity_reserved", nullable = false, precision = 12, scale = 3) @Builder.Default private BigDecimal quantityReserved = BigDecimal.ZERO;
    @Version private Long version;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
    public BigDecimal availableQuantity() { return quantityOnHand.subtract(quantityReserved); }
}
