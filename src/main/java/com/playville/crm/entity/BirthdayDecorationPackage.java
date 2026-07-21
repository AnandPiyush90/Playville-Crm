package com.playville.crm.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name = "pv_birthday_decoration_packages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BirthdayDecorationPackage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @Column(name = "package_code", nullable = false, length = 80) private String packageCode;
    @Column(name = "package_name", nullable = false, length = 150) private String packageName;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2) @Builder.Default private BigDecimal taxRate = BigDecimal.ZERO;
    @Column(nullable = false) @Builder.Default private Boolean active = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
