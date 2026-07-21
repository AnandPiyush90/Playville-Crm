package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "pv_birthday_bookings")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BirthdayBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kid_id", nullable = false)
    private Kid kid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "party_date", nullable = false)
    private LocalDate partyDate;

    @Column(name = "party_slot_start", nullable = false)
    private LocalTime partySlotStart;

    @Column(name = "party_slot_end", nullable = false)
    private LocalTime partySlotEnd;

    @Column(name = "expected_guests")
    @Builder.Default
    private Integer expectedGuests = 10;
    @Column(name = "actual_kids") private Integer actualKids;
    @Column(name = "actual_adults") private Integer actualAdults;
    @Column(name = "actual_extra_minutes") @Builder.Default private Integer actualExtraMinutes = 0;
    @Column(name = "completion_notes", columnDefinition = "TEXT") private String completionNotes;

    @Column(name = "cake_option", length = 100)
    private String cakeOption;

    @Column(name = "food_boxes_count")
    @Builder.Default
    private Integer foodBoxesCount = 0;

    @Column(name = "base_amount", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal baseAmount = BigDecimal.ZERO;

    @Column(name = "discount_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPct = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "gst_amount", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "advance_paid", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal advancePaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 20)
    @Builder.Default
    private Purchase.PaymentMode paymentMode = Purchase.PaymentMode.Cash;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.Enquiry;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum BookingStatus { Enquiry, Confirmed, Completed, Cancelled }
}