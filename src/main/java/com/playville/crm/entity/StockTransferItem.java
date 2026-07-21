package com.playville.crm.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name = "pv_stock_transfer_items") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockTransferItem {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transfer_id", nullable = false) private StockTransfer transfer;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sku_id", nullable = false) private ProductSku sku;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "batch_id") private InventoryBatch batch;
 @Column(name = "requested_quantity", nullable = false, precision = 12, scale = 3) private BigDecimal requestedQuantity;
 @Column(name = "dispatched_quantity", nullable = false, precision = 12, scale = 3) @Builder.Default private BigDecimal dispatchedQuantity = BigDecimal.ZERO;
 @Column(name = "received_quantity", nullable = false, precision = 12, scale = 3) @Builder.Default private BigDecimal receivedQuantity = BigDecimal.ZERO;
 @Column(name = "variance_reason", length = 255) private String varianceReason;
}
