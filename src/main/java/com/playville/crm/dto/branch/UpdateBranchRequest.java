package com.playville.crm.dto.branch;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class UpdateBranchRequest {

    @NotBlank(message = "Branch name is required") @Size(max = 100, message = "Branch name must be 100 characters or less")
    private String    branchName;

    @NotBlank(message = "Branch address is required")
    private String    address;

    @NotBlank(message = "City is required") @Size(max = 60)
    private String city;

    @NotBlank(message = "Official phone is required") @Size(max = 15, message = "Phone must be 15 characters or less")
    private String    phone;

    @Size(max = 15, message = "Alternate phone must be 15 characters or less")
    private String alternatePhone;

    @NotBlank(message = "Timezone is required") @Size(max = 50)
    private String timezone;

    @NotNull(message = "Opening time is required") private LocalTime openTime;
    @NotNull(message = "Closing time is required") private LocalTime closeTime;

    @Size(max = 20, message = "Closed day must be 20 characters or less")
    private String    closedDay;

    @Email(message = "Enter a valid email address")
    @Size(max = 150, message = "Notification email must be 150 characters or less")
    private String    notificationEmail;

    private Boolean emailSharingEnabled;
    @Email @Size(max = 150) private String invoiceFromEmail;
    @Email @Size(max = 150) private String invoiceReplyToEmail;
    private Boolean whatsappSharingEnabled;
    @Size(max = 50) @Pattern(regexp = "^$|^[0-9]+$", message = "WhatsApp phone number ID must contain digits only") private String whatsappPhoneNumberId;
    @Size(max = 100) @Pattern(regexp = "^$|^[a-z0-9_]+$", message = "WhatsApp template name must use lowercase letters, numbers, and underscores") private String whatsappInvoiceTemplateName;
    @Size(max = 10) @Pattern(regexp = "^$|^[a-z]{2,3}(_[A-Z]{2})?$", message = "Enter a valid language code") private String whatsappLanguageCode;

    @Size(max = 150, message = "Invoice legal name must be 150 characters or less")
    private String invoiceLegalName;

    @Size(max = 20) @Pattern(regexp = "^$|^[A-Z0-9][A-Z0-9/-]{0,19}$", message = "Invoice prefix may contain uppercase letters, numbers, slash, and hyphen")
    private String invoicePrefix;

    @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
    private java.math.BigDecimal settlementRate;

    @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", message = "Enter a valid 15-character GSTIN")
    private String gstin;

    @Pattern(regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Enter a valid PAN")
    private String panNumber;

    @Size(max = 60, message = "Tax state must be 60 characters or less")
    private String taxState;

    @Pattern(regexp = "^$|^[0-9]{2}$", message = "Tax state code must contain two digits")
    private String taxStateCode;

    @Size(max = 5000, message = "Invoice terms are too long")
    private String invoiceTerms;

    @Size(max = 500, message = "Invoice footer must be 500 characters or less")
    private String invoiceFooter;

    /** When false, this branch may onboard and check in customers without a signed disclaimer. */
    private Boolean disclaimerRequiredForPhysicalVisit;
}
