package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name="pv_email_templates",uniqueConstraints=@UniqueConstraint(name="uq_email_template_branch_key",columnNames={"branch_id","template_key"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailTemplate {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="branch_id",nullable=false) private Branch branch;
    @Column(name="template_key",nullable=false,length=60) private String templateKey;
    @Column(nullable=false,length=300) private String subject;
    @Column(name="body_text",nullable=false,columnDefinition="TEXT") private String bodyText;
    @Version @Column(nullable=false) @Builder.Default private long version=0;
    @CreationTimestamp @Column(name="created_at",updatable=false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name="updated_at") private LocalDateTime updatedAt;
}
