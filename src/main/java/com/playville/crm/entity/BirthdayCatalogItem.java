package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "pv_birthday_catalog_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BirthdayCatalogItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "item_code", nullable = false, unique = true) private String itemCode;
    @Column(nullable = false) private String category;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sku_id") private ProductSku sku;
    @Column(name = "item_name", nullable = false) private String itemName;
    @Column(length = 500) private String description;
    @Column(name = "unit_label", nullable = false) private String unitLabel;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2) private BigDecimal taxRate;
    @Column(name = "inventory_mode", nullable = false, length = 20) @Builder.Default private String inventoryMode = "NONE";
    @Column(name = "minimum_order_quantity", nullable = false, precision = 12, scale = 3) @Builder.Default private BigDecimal minimumOrderQuantity = BigDecimal.ONE;
    @Column(name = "lead_time_days", nullable = false) @Builder.Default private Integer leadTimeDays = 0;
    @Column(nullable = false) private boolean active;
}
