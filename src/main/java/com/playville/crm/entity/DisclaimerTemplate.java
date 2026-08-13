package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pv_disclaimer_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DisclaimerTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id") private Branch branch;
    @Column(name = "template_code", nullable = false) private String templateCode;
    @Column(nullable = false) private String version;
    @Column(nullable = false) private String title;
    @Column(name = "content_html", columnDefinition = "LONGTEXT", nullable = false) private String contentHtml;
    @Column(name = "content_sha256", length = 64) private String contentSha256;
    @Column(nullable = false) @Builder.Default private String status = "DRAFT";
    @Column(name = "published_at") private LocalDateTime publishedAt;
}
