package com.playville.crm.entity;

import com.playville.crm.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.*;

@Entity @Table(name = "pv_enquiries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Enquiry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "converted_customer_id") private Customer convertedCustomer;
    @Column(name = "parent_name", nullable = false, length = 150) private String parentName;
    @Column(name = "phone_number", nullable = false, length = 15) private String phoneNumber;
    @Column(length = 150) private String email;
    @Enumerated(EnumType.STRING) @Column(name = "lead_source", length = 30) private LeadSource leadSource;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) @Builder.Default private EnquiryStatus status = EnquiryStatus.NEW;
    @Column(name = "visit_scheduled_at") private LocalDateTime visitScheduledAt;
    @Column(name = "child_name", length = 100) private String childName;
    @Column(name = "child_dob") private LocalDate childDob;
    @Column(columnDefinition = "TEXT") private String notes;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
