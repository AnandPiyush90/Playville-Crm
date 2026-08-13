package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "pv_branches")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "branch_code", nullable = false, unique = true, length = 10)
    private String branchCode;

    @Column(name = "branch_name", nullable = false, length = 100)
    private String branchName;

    @Column(name = "invoice_legal_name", length = 150)
    private String invoiceLegalName;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 60)
    private String city;

    @Column(length = 15)
    private String phone;

    @Column(name = "alternate_phone", length = 15)
    private String alternatePhone;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String timezone = "Asia/Kolkata";

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "closed_day", length = 20)
    private String closedDay;

    @Column(name = "settlement_rate", precision = 5, scale = 2)
    private BigDecimal settlementRate;

    @Column(name = "notification_email", length = 150)
    private String notificationEmail;

    @Column(name = "email_sharing_enabled") private boolean emailSharingEnabled;
    @Column(name = "invoice_from_email", length = 150) private String invoiceFromEmail;
    @Column(name = "invoice_reply_to_email", length = 150) private String invoiceReplyToEmail;
    @Column(name = "whatsapp_sharing_enabled") private boolean whatsappSharingEnabled;
    @Column(name = "whatsapp_phone_number_id", length = 50) private String whatsappPhoneNumberId;
    @Column(name = "whatsapp_invoice_template_name", length = 100) private String whatsappInvoiceTemplateName;
    @Column(name = "whatsapp_language_code", nullable = false, length = 10) @Builder.Default private String whatsappLanguageCode = "en";
    @Column(name = "disclaimer_required_for_physical_visit") @Builder.Default private boolean disclaimerRequiredForPhysicalVisit = true;
    @Column(name = "tablet_signature_enabled") @Builder.Default private boolean tabletSignatureEnabled = true;
    @Column(name = "email_confirmation_enabled") @Builder.Default private boolean emailConfirmationEnabled = false;
    @Column(name = "disclaimer_email_link_ttl_hours") @Builder.Default private int disclaimerEmailLinkTtlHours = 24;
    @Column(name = "disclaimer_resign_on_new_version") @Builder.Default private boolean disclaimerResignOnNewVersion = true;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "active_disclaimer_template_id") private DisclaimerTemplate activeDisclaimerTemplate;

    @Column(length = 15)
    private String gstin;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(name = "tax_state", length = 60)
    private String taxState;

    @Column(name = "tax_state_code", length = 2)
    private String taxStateCode;

    @Column(name = "invoice_prefix", length = 20)
    private String invoicePrefix;

    @Column(name = "invoice_terms", columnDefinition = "TEXT")
    private String invoiceTerms;

    @Column(name = "invoice_footer", length = 500)
    private String invoiceFooter;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
