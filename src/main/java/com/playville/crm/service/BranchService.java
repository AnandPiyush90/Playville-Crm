package com.playville.crm.service;

import com.playville.crm.dto.branch.BranchDto;
import com.playville.crm.dto.branch.UpdateBranchRequest;
import com.playville.crm.entity.Branch;
import com.playville.crm.exception.ResourceNotFoundException;
import com.playville.crm.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<BranchDto> getAllActiveBranches() {
        return branchRepository.findAllByIsActiveTrue()
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BranchDto getBranchById(Integer id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public BranchDto updateBranch(Integer id, UpdateBranchRequest req) {
        Branch branch = findOrThrow(id);
        if (req.getGstin() != null && !req.getGstin().isBlank()
                && (req.getInvoiceLegalName() == null || req.getInvoiceLegalName().isBlank() || req.getTaxState() == null || req.getTaxState().isBlank() || req.getTaxStateCode() == null || req.getTaxStateCode().isBlank()))
            throw new com.playville.crm.exception.BusinessRuleException("GST_BILLING_DETAILS_REQUIRED: Legal name, tax state, and state code are required when GSTIN is configured");
        if (Boolean.TRUE.equals(req.getEmailSharingEnabled()) && (req.getInvoiceFromEmail() == null || req.getInvoiceFromEmail().isBlank()))
            throw new com.playville.crm.exception.BusinessRuleException("EMAIL_FROM_REQUIRED: From email is required when invoice email sharing is enabled");
        if (Boolean.TRUE.equals(req.getWhatsappSharingEnabled()) && (req.getWhatsappPhoneNumberId() == null || req.getWhatsappPhoneNumberId().isBlank()))
            throw new com.playville.crm.exception.BusinessRuleException("WHATSAPP_PHONE_NUMBER_ID_REQUIRED: Phone number ID is required when WhatsApp sharing is enabled");
        if (req.getBranchName()        != null) branch.setBranchName(req.getBranchName());
        if (req.getAddress()           != null) branch.setAddress(req.getAddress());
        if (req.getCity()              != null) branch.setCity(req.getCity().trim());
        if (req.getPhone()             != null) branch.setPhone(req.getPhone());
        if (req.getAlternatePhone()    != null) branch.setAlternatePhone(blankToNull(req.getAlternatePhone()));
        if (req.getTimezone()          != null) { try { java.time.ZoneId.of(req.getTimezone()); } catch (java.time.DateTimeException e) { throw new com.playville.crm.exception.BusinessRuleException("INVALID_TIMEZONE: Enter a valid IANA timezone"); } branch.setTimezone(req.getTimezone()); }
        if (req.getOpenTime()          != null) branch.setOpenTime(req.getOpenTime());
        if (req.getCloseTime()         != null) branch.setCloseTime(req.getCloseTime());
        if (req.getClosedDay()         != null) branch.setClosedDay(req.getClosedDay());
        if (req.getNotificationEmail() != null) branch.setNotificationEmail(req.getNotificationEmail());
        if (req.getEmailSharingEnabled() != null) branch.setEmailSharingEnabled(req.getEmailSharingEnabled());
        if (req.getInvoiceFromEmail() != null) branch.setInvoiceFromEmail(blankToNull(req.getInvoiceFromEmail()));
        if (req.getInvoiceReplyToEmail() != null) branch.setInvoiceReplyToEmail(blankToNull(req.getInvoiceReplyToEmail()));
        if (req.getWhatsappSharingEnabled() != null) branch.setWhatsappSharingEnabled(req.getWhatsappSharingEnabled());
        if (req.getWhatsappPhoneNumberId() != null) branch.setWhatsappPhoneNumberId(blankToNull(req.getWhatsappPhoneNumberId()));
        if (req.getWhatsappInvoiceTemplateName() != null) branch.setWhatsappInvoiceTemplateName(blankToNull(req.getWhatsappInvoiceTemplateName()));
        if (req.getWhatsappLanguageCode() != null) branch.setWhatsappLanguageCode(blankToNull(req.getWhatsappLanguageCode()) == null ? "en" : req.getWhatsappLanguageCode());
        if (req.getInvoiceLegalName()  != null) branch.setInvoiceLegalName(blankToNull(req.getInvoiceLegalName()));
        if (req.getInvoicePrefix()     != null) branch.setInvoicePrefix(upperOrNull(req.getInvoicePrefix()));
        if (req.getSettlementRate()    != null) branch.setSettlementRate(req.getSettlementRate());
        if (req.getGstin()             != null) branch.setGstin(upperOrNull(req.getGstin()));
        if (req.getPanNumber()         != null) branch.setPanNumber(upperOrNull(req.getPanNumber()));
        if (req.getTaxState()          != null) branch.setTaxState(blankToNull(req.getTaxState()));
        if (req.getTaxStateCode()      != null) branch.setTaxStateCode(blankToNull(req.getTaxStateCode()));
        if (req.getInvoiceTerms()      != null) branch.setInvoiceTerms(blankToNull(req.getInvoiceTerms()));
        if (req.getInvoiceFooter()     != null) branch.setInvoiceFooter(blankToNull(req.getInvoiceFooter()));
        return toDto(branchRepository.save(branch));
    }

    private Branch findOrThrow(Integer id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
    }

    private BranchDto toDto(Branch b) {
        return BranchDto.builder()
                .id(b.getId())
                .branchCode(b.getBranchCode())
                .branchName(b.getBranchName())
                .invoiceLegalName(b.getInvoiceLegalName())
                .address(b.getAddress())
                .city(b.getCity())
                .phone(b.getPhone())
                .alternatePhone(b.getAlternatePhone())
                .timezone(b.getTimezone())
                .openTime(b.getOpenTime())
                .closeTime(b.getCloseTime())
                .closedDay(b.getClosedDay())
                .settlementRate(b.getSettlementRate())
                .notificationEmail(b.getNotificationEmail())
                .emailSharingEnabled(b.isEmailSharingEnabled()).invoiceFromEmail(b.getInvoiceFromEmail()).invoiceReplyToEmail(b.getInvoiceReplyToEmail())
                .whatsappSharingEnabled(b.isWhatsappSharingEnabled()).whatsappPhoneNumberId(b.getWhatsappPhoneNumberId())
                .whatsappInvoiceTemplateName(b.getWhatsappInvoiceTemplateName()).whatsappLanguageCode(b.getWhatsappLanguageCode())
                .gstin(b.getGstin())
                .panNumber(b.getPanNumber())
                .taxState(b.getTaxState())
                .taxStateCode(b.getTaxStateCode())
                .invoicePrefix(b.getInvoicePrefix())
                .invoiceTerms(b.getInvoiceTerms())
                .invoiceFooter(b.getInvoiceFooter())
                .isActive(b.isActive())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String upperOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase(java.util.Locale.ROOT);
    }
}
