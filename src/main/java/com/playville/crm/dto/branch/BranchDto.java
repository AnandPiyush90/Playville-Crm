package com.playville.crm.dto.branch;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Builder
public class BranchDto {
    private Integer    id;
    private String     branchCode;
    private String     branchName;
    private String     invoiceLegalName;
    private String     address;
    private String     city;
    private String     phone;
    private String     alternatePhone;
    private String     timezone;
    private LocalTime  openTime;
    private LocalTime  closeTime;
    private String     closedDay;
    private BigDecimal settlementRate;
    private String     notificationEmail;
    private boolean emailSharingEnabled;
    private String invoiceFromEmail;
    private String invoiceReplyToEmail;
    private boolean whatsappSharingEnabled;
    private String whatsappPhoneNumberId;
    private String whatsappInvoiceTemplateName;
    private String whatsappLanguageCode;
    private String     gstin;
    private String     panNumber;
    private String     taxState;
    private String     taxStateCode;
    private String     invoicePrefix;
    private String     invoiceTerms;
    private String     invoiceFooter;
    private boolean    isActive;
}
