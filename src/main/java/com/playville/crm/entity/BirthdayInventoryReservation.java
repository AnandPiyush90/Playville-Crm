package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "pv_birthday_inventory_reservations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BirthdayInventoryReservation {
    public enum Status { RESERVED, RELEASED, CONSUMED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "birthday_booking_id", nullable = false) private BirthdayBooking booking;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "birthday_quote_line_id", nullable = false) private BirthdayQuoteLine quoteLine;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sku_id", nullable = false) private ProductSku sku;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal quantity;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) @Builder.Default private Status status = Status.RESERVED;
    @CreationTimestamp @Column(name = "reserved_at", updatable = false) private LocalDateTime reservedAt;
    @Column(name = "released_at") private LocalDateTime releasedAt;
    @Column(name = "consumed_at") private LocalDateTime consumedAt;
}
