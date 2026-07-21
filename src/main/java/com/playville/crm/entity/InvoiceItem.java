package com.playville.crm.entity;

import com.playville.crm.entity.enums.InvoiceLineType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_invoice_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invoice_id", nullable = false) private Invoice invoice;
    @Column(name = "line_number", nullable = false) private Integer lineNumber;
    @Enumerated(EnumType.STRING) @Column(name = "line_type", nullable = false, length = 30) private InvoiceLineType lineType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id") private Product product;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sku_id") private ProductSku sku;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "package_id") private PlayvillePackage playvillePackage;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "purchase_id") private Purchase purchase;
    @Column(name = "description_snapshot", nullable = false, length = 200) private String descriptionSnapshot;
    @Column(name = "sku_snapshot", length = 100) private String skuSnapshot;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2) private BigDecimal grossAmount;
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name = "taxable_amount", nullable = false, precision = 12, scale = 2) private BigDecimal taxableAmount;
    @Column(name = "tax_rate_snapshot", nullable = false, precision = 5, scale = 2) @Builder.Default private BigDecimal taxRateSnapshot = BigDecimal.ZERO;
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal taxAmount = BigDecimal.ZERO;
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2) private BigDecimal lineTotal;
    @Column(name = "returned_quantity", nullable = false, precision = 12, scale = 3) @Builder.Default private BigDecimal returnedQuantity = BigDecimal.ZERO;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
