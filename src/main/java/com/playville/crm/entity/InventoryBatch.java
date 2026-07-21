package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name = "pv_inventory_batches") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sku_id", nullable = false) private ProductSku sku;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "stock_receipt_id") private StockReceipt stockReceipt;
    @Column(name = "batch_number", length = 100) private String batchNumber;
    @Column(name = "received_at", nullable = false) private LocalDateTime receivedAt;
    @Column(name = "manufactured_on") private LocalDate manufacturedOn;
    @Column(name = "expires_on") private LocalDate expiresOn;
    @Column(name = "unit_cost", precision = 10, scale = 2) private BigDecimal unitCost;
    @Column(name = "quantity_received", nullable = false, precision = 12, scale = 3) private BigDecimal quantityReceived;
    @Column(name = "quantity_remaining", nullable = false, precision = 12, scale = 3) private BigDecimal quantityRemaining;
    @Column(nullable = false, length = 20) @Builder.Default private String status = "ACTIVE";
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
