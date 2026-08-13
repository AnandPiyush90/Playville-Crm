package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name="pv_email_outbox")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailOutbox {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="delivery_id",nullable=false,unique=true) private NotificationDelivery delivery;
    @Column(name="encrypted_subject",nullable=false,columnDefinition="TEXT") private String encryptedSubject;
    @Column(name="encrypted_body",nullable=false,columnDefinition="LONGTEXT") private String encryptedBody;
    @Column(name="attachment_filename",length=255) private String attachmentFilename;
    @Lob @Column(name="attachment_bytes") private byte[] attachmentBytes;
    @Column(nullable=false,length=20) @Builder.Default private String status="PENDING";
    @Column(name="next_attempt_at",nullable=false) private LocalDateTime nextAttemptAt;
    @Column(name="locked_at") private LocalDateTime lockedAt;
    @CreationTimestamp @Column(name="created_at",updatable=false) private LocalDateTime createdAt;
}
