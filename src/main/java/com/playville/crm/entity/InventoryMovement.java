package com.playville.crm.entity;

import com.playville.crm.entity.enums.InventoryMovementType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_inventory_movements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sku_id", nullable = false) private ProductSku sku;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "batch_id") private InventoryBatch batch;
    @Enumerated(EnumType.STRING) @Column(name = "movement_type", nullable = false, length = 30) private InventoryMovementType movementType;
    @Column(name = "quantity_delta", nullable = false, precision = 12, scale = 3) private BigDecimal quantityDelta;
    @Column(name = "unit_cost_snapshot", precision = 10, scale = 2) private BigDecimal unitCostSnapshot;
    @Column(name = "reference_type", nullable = false, length = 40) private String referenceType;
    @Column(name = "reference_id") private Integer referenceId;
    @Column(name = "reason_code", length = 60) private String reasonCode;
    @Column(columnDefinition = "TEXT") private String notes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "performed_by_staff_id") private Staff performedByStaff;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "approved_by_staff_id") private Staff approvedByStaff;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
