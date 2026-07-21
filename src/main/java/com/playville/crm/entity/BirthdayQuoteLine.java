package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "pv_birthday_quote_lines")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class BirthdayQuoteLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "birthday_booking_id", nullable = false)
    private BirthdayBooking birthdayBooking;
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;
    @Column(nullable = false)
    private String category;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "catalog_item_id")
    private BirthdayCatalogItem catalogItem;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sku_id")
    private ProductSku sku;
    @Column(name = "inventory_reservation_required", nullable = false)
    private boolean inventoryReservationRequired;
    @Column(name = "description_snapshot", nullable = false)
    private String descriptionSnapshot;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;
}
