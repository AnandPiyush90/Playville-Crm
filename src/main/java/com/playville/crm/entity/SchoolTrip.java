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
@Table(name = "pv_school_trips")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "school_name", nullable = false, length = 200)
    private String schoolName;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(name = "contact_phone", length = 15)
    private String contactPhone;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @Column(name = "slot_start")
    private LocalTime slotStart;

    @Column(name = "slot_end")
    private LocalTime slotEnd;

    @Column(name = "expected_kids")
    @Builder.Default
    private Integer expectedKids = 0;

    @Column(name = "actual_kids")
    @Builder.Default
    private Integer actualKids = 0;

    @Column(name = "price_per_kid", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal pricePerKid = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "gst_amount", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "advance_paid", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal advancePaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 20)
    @Builder.Default
    private Purchase.PaymentMode paymentMode = Purchase.PaymentMode.Online;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private BirthdayBooking.BookingStatus status = BirthdayBooking.BookingStatus.Enquiry;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}