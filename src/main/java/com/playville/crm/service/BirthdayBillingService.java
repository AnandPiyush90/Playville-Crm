package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.invoice.InvoiceResponse;
import com.playville.crm.dto.invoice.PaymentRequest;
import com.playville.crm.entity.*;
import com.playville.crm.entity.enums.*;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class BirthdayBillingService {
    private final BirthdayBookingRepository bookingRepository;
    private final BirthdayQuoteLineRepository quoteLineRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository paymentRepository;
    private final StaffRepository staffRepository;
    private final InvoiceService invoiceService;

    @Transactional
    public InvoiceResponse issue(Integer bookingId, String username) {
        BirthdayBooking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("Birthday booking", bookingId));
        enforceBranch(booking);
        Invoice existing = invoiceRepository.findByBirthdayBookingId(bookingId).orElse(null);
        if (existing != null) return invoiceService.get(existing.getId());
        List<BirthdayQuoteLine> lines = quoteLineRepository.findByBirthdayBookingIdOrderByLineNumberAsc(bookingId);
        if (lines.isEmpty()) throw new BusinessRuleException("BIRTHDAY_QUOTE_REQUIRED: Save the birthday quote before issuing its invoice");
        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username).orElse(null);
        Invoice invoice = Invoice.builder().branch(booking.getBranch()).customer(booking.getCustomer()).birthdayBooking(booking)
                .invoiceType("BIRTHDAY_BOOKING").invoiceDate(LocalDateTime.now()).status(InvoiceStatus.UNPAID).issuedByStaff(staff)
                .customerNameSnapshot(booking.getCustomer().getParentName()).customerPhoneSnapshot(booking.getCustomer().getPhoneNumber())
                .customerEmailSnapshot(booking.getCustomer().getEmail()).subtotal(BigDecimal.ZERO).discountTotal(BigDecimal.ZERO)
                .taxTotal(BigDecimal.ZERO).roundingAdjustment(BigDecimal.ZERO).grandTotal(BigDecimal.ZERO).amountPaid(BigDecimal.ZERO).balanceDue(BigDecimal.ZERO).build();
        BigDecimal subtotal = BigDecimal.ZERO, tax = BigDecimal.ZERO;
        for (BirthdayQuoteLine quote : lines) {
            BigDecimal gross = money(quote.getLineTotal());
            BigDecimal lineTax = money(gross.multiply(quote.getTaxRate()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            invoice.getItems().add(InvoiceItem.builder().invoice(invoice).lineNumber(quote.getLineNumber()).lineType(InvoiceLineType.SERVICE)
                    .descriptionSnapshot(quote.getDescriptionSnapshot()).quantity(quote.getQuantity()).unitPrice(money(quote.getUnitPrice()))
                    .grossAmount(gross).discountAmount(BigDecimal.ZERO).taxableAmount(gross).taxRateSnapshot(quote.getTaxRate()).taxAmount(lineTax).lineTotal(gross.add(lineTax)).build());
            subtotal = subtotal.add(gross); tax = tax.add(lineTax);
        }
        invoice.setSubtotal(money(subtotal)); invoice.setTaxTotal(money(tax)); invoice.setGrandTotal(money(subtotal.add(tax))); invoice.setBalanceDue(invoice.getGrandTotal());
        BigDecimal advance = money(booking.getAdvancePaid());
        if (advance.compareTo(invoice.getGrandTotal()) > 0) throw new BusinessRuleException("ADVANCE_EXCEEDS_TOTAL: Advance cannot exceed the birthday invoice total");
        if (advance.signum() > 0) {
            invoice.getPayments().add(InvoicePayment.builder().invoice(invoice).branch(invoice.getBranch()).paymentType("ADVANCE")
                    .paymentMode(mapMode(booking.getPaymentMode())).amount(advance).providerReference(booking.getPaymentReference()).receivedByStaff(staff)
                    .paidAt(LocalDateTime.now()).status("COMPLETED").build());
            invoice.setAmountPaid(advance); invoice.setBalanceDue(money(invoice.getGrandTotal().subtract(advance)));
            invoice.setStatus(invoice.getBalanceDue().signum() == 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
        }
        invoiceService.prepareIssuedInvoice(invoice);
        invoiceRepository.saveAndFlush(invoice); return invoiceService.get(invoice.getId());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Integer bookingId) {
        Invoice invoice = invoiceRepository.findByBirthdayBookingId(bookingId).orElseThrow(() -> new ResourceNotFoundException("Birthday invoice", bookingId));
        if (!invoice.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
        return invoiceService.get(invoice.getId());
    }

    @Transactional
    public InvoiceResponse recordPayment(Integer bookingId, PaymentRequest request, String username, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw new BusinessRuleException("IDEMPOTENCY_KEY_REQUIRED: Idempotency-Key header is required to record a payment");
        Invoice invoice = invoiceRepository.findByBirthdayBookingId(bookingId).orElseThrow(() -> new BusinessRuleException("BIRTHDAY_INVOICE_REQUIRED: Issue the birthday invoice before recording a payment"));
        invoice = invoiceRepository.findByIdForUpdate(invoice.getId()).orElseThrow(() -> new ResourceNotFoundException("Invoice", bookingId));
        if (!invoice.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
        BigDecimal amount = money(request.getAmount());
        if (amount.compareTo(invoice.getBalanceDue()) > 0) throw new BusinessRuleException("PAYMENT_EXCEEDS_BALANCE: Payment cannot exceed the pending amount");
        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username).orElse(null);
        paymentRepository.save(InvoicePayment.builder().invoice(invoice).branch(invoice.getBranch()).paymentType("PAYMENT").paymentMode(request.getPaymentMode())
                .amount(amount).providerReference(request.getProviderReference()).receivedByStaff(staff).paidAt(LocalDateTime.now()).status("COMPLETED").idempotencyKey(idempotencyKey.trim()).build());
        invoice.setAmountPaid(money(invoice.getAmountPaid().add(amount))); invoice.setBalanceDue(money(invoice.getGrandTotal().subtract(invoice.getAmountPaid())));
        invoice.setStatus(invoice.getBalanceDue().signum() == 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
        BirthdayBooking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("Birthday booking", bookingId));
        booking.setAdvancePaid(invoice.getAmountPaid());
        invoiceRepository.saveAndFlush(invoice); return invoiceService.get(invoice.getId());
    }
    private void enforceBranch(BirthdayBooking booking) { if (!booking.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException(); }
    private InvoicePaymentMode mapMode(Purchase.PaymentMode mode) { return InvoicePaymentMode.valueOf((mode == null ? Purchase.PaymentMode.Cash : mode).name().toUpperCase()); }
    private BigDecimal money(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP); }
}
