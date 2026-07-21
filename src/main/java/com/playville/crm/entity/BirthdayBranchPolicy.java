package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "pv_birthday_branch_policy_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BirthdayBranchPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @Column(name = "extra_kid_price", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal extraKidPrice = new BigDecimal("400.00");
    @Column(name = "extra_adult_price", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal extraAdultPrice = new BigDecimal("200.00");
    @Column(name = "extra_time_30_min_price", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal extraTime30MinPrice = new BigDecimal("1000.00");
    @Column(name = "enquiry_calendar_enabled", nullable = false) @Builder.Default private Boolean enquiryCalendarEnabled = true;
    @Column(name = "cancellation_policy_json", columnDefinition = "TEXT") private String cancellationPolicyJson;
    @Column(name = "refund_policy_json", columnDefinition = "TEXT") private String refundPolicyJson;
    @Column(name = "share_channel", length = 30) private String shareChannel;
    @Column(name = "share_settings_json", columnDefinition = "TEXT") private String shareSettingsJson;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
