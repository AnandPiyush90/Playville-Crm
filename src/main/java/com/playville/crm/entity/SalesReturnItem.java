package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "pv_sales_return_items") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalesReturnItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sales_return_id", nullable = false) private SalesReturn salesReturn;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "original_invoice_item_id", nullable = false) private InvoiceItem originalInvoiceItem;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal quantity;
    @Builder.Default private boolean resellable = false;
}
