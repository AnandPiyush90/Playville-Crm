package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_product_skus")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductSku {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id", nullable = false) private Product product;
    @Column(name = "sku_code", nullable = false, unique = true, length = 80) private String skuCode;
    @Column(unique = true, length = 100) private String barcode;
    @Column(name = "variant_attributes_json", columnDefinition = "json") private String variantAttributesJson;
    @Column(name = "unit_of_measure", nullable = false, length = 20) @Builder.Default private String unitOfMeasure = "PIECE";
    @Column(name = "default_sale_price", nullable = false, precision = 10, scale = 2) private BigDecimal defaultSalePrice;
    @Column(name = "default_cost_price", precision = 10, scale = 2) private BigDecimal defaultCostPrice;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "tax_profile_id") private TaxProfile taxProfile;
    @Column(name = "has_expiry") @Builder.Default private boolean hasExpiry = false;
    @Column(name = "is_active") @Builder.Default private boolean isActive = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
