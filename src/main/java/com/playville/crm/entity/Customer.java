package com.playville.crm.entity;

import com.playville.crm.entity.enums.LeadSource;
import com.playville.crm.entity.enums.LeadSourceConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pv_customers")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 15)
    private String phoneNumber;

    @Column(name = "parent_name", nullable = false, length = 150)
    private String parentName;

    @Column(length = 150)
    private String email;

    @Convert(converter = LeadSourceConverter.class)
    @Column(name = "lead_source", length = 20)
    @Builder.Default
    private LeadSource leadSource = LeadSource.Walk_in;

    @Column(name = "global_session_balance")
    @Builder.Default
    private Integer globalSessionBalance = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_package_id")
    private PlayvillePackage currentPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_branch_id", nullable = false)
    private Branch homeBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_branch_id")
    private Branch purchaseBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_visit_branch_id")
    private Branch firstVisitBranch;

    @Column(name = "disclaimer_accepted")
    @Builder.Default
    private boolean disclaimerAccepted = false;

    @Column(name = "acceptance_timestamp")
    private LocalDateTime acceptanceTimestamp;

    @Column(name = "portal_pin", length = 4)
    private String portalPin;

    @Column(name = "total_visits")
    @Builder.Default
    private Integer totalVisits = 0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @OneToMany(mappedBy = "customer",
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY,
               orphanRemoval = true)
    @Builder.Default
    private List<Kid> kids = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}