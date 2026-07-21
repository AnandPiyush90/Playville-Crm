package com.playville.crm.entity;

import com.playville.crm.entity.enums.ProductType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pv_products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "product_code", nullable = false, unique = true, length = 60) private String productCode;
    @Column(name = "product_name", nullable = false, length = 150) private String productName;
    @Column(columnDefinition = "TEXT") private String description;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id", nullable = false) private ProductCategory category;
    @Enumerated(EnumType.STRING) @Column(name = "product_type", nullable = false, length = 30) private ProductType productType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "package_id") private PlayvillePackage playvillePackage;
    @Column(name = "track_inventory") @Builder.Default private boolean trackInventory = true;
    @Column(name = "is_active") @Builder.Default private boolean isActive = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
