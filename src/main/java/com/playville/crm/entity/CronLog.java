package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pv_cron_log")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CronLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "run_at")
    private LocalDateTime runAt;

    @Column(name = "run_end_at")
    private LocalDateTime runEndAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "checkins_closed")
    @Builder.Default
    private Integer checkinsClosedCount = 0;

    @Column(name = "sessions_deducted")
    @Builder.Default
    private Integer sessionsDeducted = 0;

    @Column(name = "notifications_sent")
    @Builder.Default
    private Integer notificationsSent = 0;

    @Column(name = "error_count")
    @Builder.Default
    private Integer errorCount = 0;

    @Column(name = "errors", columnDefinition = "TEXT")
    private String errors;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private CronStatus status = CronStatus.Success;

    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", length = 20)
    @Builder.Default
    private TriggerSource triggeredBy = TriggerSource.Scheduler;

    public enum CronStatus    { Success, Partial, Failed }
    public enum TriggerSource { Scheduler, Manual }
}