package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.playville.crm.entity.enums.*;

@Entity
@Table(name = "pv_checkins")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Checkin {

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
    @JoinColumn(name = "entitlement_id")
    private CustomerEntitlement entitlement;

    @Column(name = "visit_type", length = 40)
    @Builder.Default
    private String visitType = "PAID";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "checkin_time")
    private LocalDateTime checkinTime;

    @Column(name = "checkout_time")
    private LocalDateTime checkoutTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private CheckinStatus status = CheckinStatus.Active;

    @Column(name = "kids_count")
    @Builder.Default
    private Integer kidsCount = 0;

    @Column(name = "sessions_deducted")
    @Builder.Default
    private Integer sessionsDeducted = 0;

    @Column(name = "extra_charges", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal extraCharges = BigDecimal.ZERO;

    @Column(name = "gst_amount", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "total_charged", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal totalCharged = BigDecimal.ZERO;

    @Column(name = "checkout_notes", columnDefinition = "TEXT")
    private String checkoutNotes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Enumerated(EnumType.STRING) @Column(name = "conversion_outcome", length = 30)
    private ConversionOutcome conversionOutcome;
    @Enumerated(EnumType.STRING) @Column(name = "conversion_reason", length = 30)
    private ConversionReason conversionReason;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conversion_purchase_id")
    private Purchase conversionPurchase;
    @Column(name = "follow_up_at") private LocalDateTime followUpAt;

    @Column(name = "auto_closed")
    @Builder.Default
    private boolean autoClosed = false;

    @Column(name = "google_chat_sent")
    @Builder.Default
    private boolean googleChatSent = false;

    @OneToMany(mappedBy = "checkin",
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY,
               orphanRemoval = true)
    @Builder.Default
    private List<CheckinKid> checkinKids = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum CheckinStatus { Active, Completed, Auto_Closed, Cancelled }
}
