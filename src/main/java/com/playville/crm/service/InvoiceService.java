package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.invoice.*;
import com.playville.crm.entity.*;
import com.playville.crm.entity.enums.*;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.math.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final ProductSkuRepository skuRepository;
    private final BranchSkuRepository branchSkuRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryMovementRepository movementRepository;
    private final CustomerRepository customerRepository;
    private final CheckinRepository checkinRepository;
    private final BranchRepository branchRepository;
    private final StaffRepository staffRepository;
    private final InvoiceSequenceRepository sequenceRepository;
    private final PlayvillePackageRepository packageRepository;
    private final PurchaseRepository purchaseRepository;
    private final InventoryBatchRepository batchRepository;
    private final CheckinService checkinService;
    private final SalesReturnRepository salesReturnRepository;
    private final SalesReturnItemRepository salesReturnItemRepository;
    private final InvoicePaymentRepository paymentRepository;
    private final CheckinSessionItemRepository sessionItemRepository;

    @Transactional
    public InvoiceResponse ensureCheckoutDraft(Integer checkinId) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin", checkinId));
        enforceCheckinBranch(checkin);
        if (checkin.getStatus() != Checkin.CheckinStatus.Active)
            throw new BusinessRuleException("CHECKIN_NOT_ACTIVE: Checkout invoice can only be created for an active visit");
        List<CheckinSessionItem> visitItems = sessionItemRepository
                .findByCheckinIdAndStatusOrderByCreatedAtAsc(checkinId, CheckinSessionItem.Status.OPEN);
        if (visitItems.isEmpty())
            throw new BusinessRuleException("VISIT_ITEMS_REQUIRED: Add at least one product to the visit before creating its invoice");
        Invoice invoice = invoiceRepository.findFirstByCheckinIdAndStatusOrderByIdDesc(checkinId, InvoiceStatus.DRAFT)
                .orElseGet(() -> {
                    CreateInvoiceRequest create = new CreateInvoiceRequest();
                    create.setCheckinId(checkinId);
                    create.setNotes("Items used during check-in #" + checkinId);
                    create.setItems(List.of());
                    createDraft(create);
                    return invoiceRepository.findFirstByCheckinIdAndStatusOrderByIdDesc(checkinId, InvoiceStatus.DRAFT)
                            .orElseThrow(() -> new IllegalStateException("Checkout draft was not created"));
                });
        List<InvoiceLineRequest> lines = visitItems.stream().map(item -> {
            InvoiceLineRequest line = new InvoiceLineRequest();
            line.setLineType(InvoiceLineType.RETAIL);
            line.setSkuId(item.getSku().getId());
            line.setQuantity(item.getQuantity());
            return line;
        }).toList();
        replaceLines(invoice, lines);
        Map<Integer, CheckinSessionItem> snapshots = new HashMap<>();
        visitItems.forEach(item -> snapshots.put(item.getSku().getId(), item));
        invoice.getItems().forEach(line -> {
            CheckinSessionItem source = snapshots.get(line.getSku().getId());
            line.setDescriptionSnapshot(source.getProductNameSnapshot());
            line.setSkuSnapshot(source.getSkuCodeSnapshot());
            line.setUnitPrice(source.getUnitPriceSnapshot());
            line.setGrossAmount(source.getUnitPriceSnapshot().multiply(source.getQuantity()).setScale(2, RoundingMode.HALF_UP));
            line.setTaxRateSnapshot(source.getTaxRateSnapshot());
            line.setTaxableAmount(source.getTaxableAmount());
            line.setTaxAmount(source.getTaxAmount());
            line.setLineTotal(source.getLineTotal());
        });
        recalculate(invoice);
        invoiceRepository.saveAndFlush(invoice);
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse createDraft(CreateInvoiceRequest request) {
        Integer branchId = BranchContext.getBranchId();
        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        Customer customer = request.getCustomerId() == null ? null : customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));
        Checkin checkin = request.getCheckinId() == null ? null : checkinRepository.findById(request.getCheckinId())
                .orElseThrow(() -> new ResourceNotFoundException("Checkin", request.getCheckinId()));
        if (checkin != null) {
            if (!checkin.getBranch().getId().equals(branchId)) throw new BranchAccessDeniedException();
            if (customer != null && !checkin.getCustomer().getId().equals(customer.getId())) throw new BusinessRuleException("CHECKIN_CUSTOMER_MISMATCH: Check-in must belong to the selected customer");
            customer = checkin.getCustomer();
        }
        Invoice invoice = Invoice.builder().branch(branch).customer(customer).checkin(checkin).notes(request.getNotes()).build();
        captureSellerSnapshot(invoice, branch);
        captureCustomerSnapshot(invoice, customer);
        invoiceRepository.save(invoice);
        if (request.getItems() != null && !request.getItems().isEmpty()) replaceLines(invoice, request.getItems());
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(Integer invoiceId) {
        Invoice invoice = invoiceRepository.findDetailById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        enforceBranch(invoice);
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse ensurePurchaseReceipt(Purchase purchase, String username) {
        Invoice existing = invoiceRepository.findByPurchaseId(purchase.getId()).orElse(null);
        if (existing != null) {
            enforceBranch(existing);
            return toResponse(existing);
        }
        if (!purchase.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username).orElse(purchase.getStaff());
        BigDecimal total = money(purchase.getAmountPaid());
        BigDecimal tax = money(purchase.getGstAmount()).min(total);
        BigDecimal taxable = money(total.subtract(tax));
        Invoice invoice = Invoice.builder().branch(purchase.getBranch()).customer(purchase.getCustomer())
                .invoiceType("MEMBERSHIP_PURCHASE").invoiceNumber(nextInvoiceNumber(purchase.getBranch()))
                .invoiceDate(LocalDateTime.now()).status(InvoiceStatus.PAID).issuedByStaff(staff)
                .customerNameSnapshot(purchase.getCustomer().getParentName()).customerPhoneSnapshot(purchase.getCustomer().getPhoneNumber())
                .customerEmailSnapshot(purchase.getCustomer().getEmail()).subtotal(taxable)
                .discountTotal(money(purchase.getDiscountApplied())).taxTotal(tax).grandTotal(total)
                .amountPaid(total).balanceDue(BigDecimal.ZERO.setScale(2)).notes(purchase.getNotes()).build();
        captureSellerSnapshot(invoice, purchase.getBranch());
        PlayvillePackage pkg = purchase.getPlayvillePackage();
        invoice.getItems().add(InvoiceItem.builder().invoice(invoice).lineNumber(1).lineType(InvoiceLineType.MEMBERSHIP)
                .playvillePackage(pkg).purchase(purchase).descriptionSnapshot(purchaseLineDescription(purchase, pkg))
                .quantity(BigDecimal.ONE).unitPrice(total).grossAmount(total).discountAmount(money(purchase.getDiscountApplied()))
                .taxableAmount(taxable).taxRateSnapshot(new BigDecimal("18.00")).taxAmount(tax).lineTotal(total).build());
        invoice.getPayments().add(InvoicePayment.builder().invoice(invoice).branch(purchase.getBranch()).paymentType("PAYMENT")
                .paymentMode(toInvoicePaymentMode(purchase.getPaymentMode())).amount(total).providerReference(blankToNull(purchase.getPaymentReference()))
                .status("COMPLETED").receivedByStaff(staff).paidAt(purchase.getCreatedAt() == null ? LocalDateTime.now() : purchase.getCreatedAt())
                .idempotencyKey("purchase-receipt-" + purchase.getId()).build());
        invoiceRepository.saveAndFlush(invoice);
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> list(LocalDate from, LocalDate to, InvoiceStatus status, Integer customerId, InvoicePaymentMode paymentMode, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        LocalDate safeFrom = from == null ? null : from;
        LocalDate safeTo = to == null ? null : to;
        if (safeFrom != null && safeTo != null && safeTo.isBefore(safeFrom)) {
            throw new BusinessRuleException("INVALID_DATE_RANGE: To date must be on or after from date");
        }
        if (safeFrom != null && safeTo != null && safeFrom.plusDays(366).isBefore(safeTo)) {
            throw new BusinessRuleException("DATE_RANGE_TOO_LARGE: Invoice searches are limited to 366 days");
        }
        return invoiceRepository.search(
                BranchContext.getBranchId(),
                safeFrom == null ? null : safeFrom.atStartOfDay(),
                safeTo == null ? null : safeTo.plusDays(1).atStartOfDay(),
                status,
                customerId,
                paymentMode,
                pageable).map(this::toResponse);
    }

    @Transactional
    public InvoiceResponse replaceItems(Integer invoiceId, ReplaceInvoiceItemsRequest request) {
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        enforceBranch(invoice);
        requireDraftAndVersion(invoice, request.getVersion());
        replaceLines(invoice, request.getItems());
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse finalizeInvoice(Integer invoiceId, FinalizeInvoiceRequest request, String username, String idempotencyKey) {
        String key = blankToNull(idempotencyKey);
        if (key == null) throw new BusinessRuleException("IDEMPOTENCY_KEY_REQUIRED: Idempotency-Key header is required to finalize an invoice");
        Optional<Invoice> alreadyFinalized = invoiceRepository.findByIdempotencyKey(key);
        if (alreadyFinalized.isPresent()) {
            enforceBranch(alreadyFinalized.get());
            if (!alreadyFinalized.get().getId().equals(invoiceId)) {
                throw new ApiConflictException("IDEMPOTENCY_KEY_REUSED", "Idempotency-Key was already used for a different invoice");
            }
            return toResponse(alreadyFinalized.get());
        }
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        enforceBranch(invoice);
        if (invoice.getIdempotencyKey() != null) {
            if (invoice.getIdempotencyKey().equals(key)) return toResponse(invoice);
            throw new ApiConflictException("INVOICE_ALREADY_FINALIZED", "Invoice has already been finalized");
        }
        requireDraftAndVersion(invoice, request.getVersion());
        if (invoice.getItems().isEmpty()) throw new BusinessRuleException("INVOICE_ITEMS_REQUIRED: At least one invoice item is required");
        List<PaymentRequest> payments = request.getPayments() == null ? List.of() : request.getPayments();
        validatePaymentReferences(payments);
        BigDecimal paid = payments.stream().map(PaymentRequest::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        if (paid.compareTo(invoice.getGrandTotal()) > 0) throw new ApiConflictException("PAYMENT_EXCEEDS_INVOICE_TOTAL", "Payment total cannot exceed the invoice grand total");
        if (Boolean.TRUE.equals(request.getCompleteCheckout()) && paid.compareTo(invoice.getGrandTotal()) != 0)
            throw new BusinessRuleException("CHECKOUT_PAYMENT_REQUIRED: Full invoice payment is required before completing checkout");
        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username).orElse(null);
        Customer lockedCustomer = null;
        List<InvoiceItem> membershipItems = invoice.getItems().stream().filter(i -> i.getLineType() == InvoiceLineType.MEMBERSHIP).toList();
        if (!membershipItems.isEmpty()) {
            if (invoice.getCustomer() == null) throw new BusinessRuleException("CUSTOMER_REQUIRED: A customer is required for membership purchases");
            if (membershipItems.size() > 1 && invoice.getCheckin() != null) throw new BusinessRuleException("MULTIPLE_MEMBERSHIPS_NOT_ALLOWED: A check-in can be associated with only one membership purchase");
            lockedCustomer = customerRepository.findByIdForUpdate(invoice.getCustomer().getId()).orElseThrow(() -> new ResourceNotFoundException("Customer", invoice.getCustomer().getId()));
        }
        Map<Integer, InventoryBalance> lockedBalances = new HashMap<>();
        List<InvoiceItem> retailItems = invoice.getItems().stream().filter(i -> i.getLineType() == InvoiceLineType.RETAIL).sorted(Comparator.comparing(i -> i.getSku().getId())).toList();
        List<InvoiceItem> stockItems = retailItems.stream().filter(i -> i.getSku().getProduct().isTrackInventory()).toList();
        if (Boolean.TRUE.equals(request.getCompleteCheckout())) claimVisitItemReservations(invoice, retailItems);
        for (InvoiceItem item : stockItems) {
            ProductSku sku = item.getSku();
            BranchSku branchSku = branchSkuRepository.findByBranchIdAndSkuId(invoice.getBranch().getId(), sku.getId())
                    .orElseThrow(() -> new ApiConflictException("SKU_NOT_AVAILABLE_AT_BRANCH", "SKU is not available at this branch"));
            if (!branchSku.isAvailable() || !sku.isActive() || !sku.getProduct().isActive()) throw new ApiConflictException("PRODUCT_INACTIVE", "Product is no longer available for sale");
            InventoryBalance balance = lockedBalances.computeIfAbsent(sku.getId(), ignored -> balanceRepository
                    .findByBranchIdAndSkuIdForUpdate(invoice.getBranch().getId(), sku.getId())
                    .orElseThrow(() -> new ApiConflictException("INSUFFICIENT_STOCK", "No stock is available for " + sku.getSkuCode())));
            if (!branchSku.isAllowNegativeStock() && balance.availableQuantity().compareTo(item.getQuantity()) < 0)
                throw new ApiConflictException("INSUFFICIENT_STOCK", "Insufficient stock for " + sku.getSkuCode());
            balance.setQuantityOnHand(balance.getQuantityOnHand().subtract(item.getQuantity()));
        }
        lockedBalances.values().forEach(balanceRepository::save);
        List<SaleAllocation> saleAllocations = new ArrayList<>();
        for (InvoiceItem item : stockItems) saleAllocations.addAll(allocateStock(invoice, item));
        invoice.setInvoiceNumber(nextInvoiceNumber(invoice.getBranch()));
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setAmountPaid(paid);
        invoice.setBalanceDue(money(invoice.getGrandTotal().subtract(paid)));
        invoice.setStatus(paymentStatus(invoice.getBalanceDue(), invoice.getGrandTotal()));
        invoice.setIssuedByStaff(staff);
        invoice.setIdempotencyKey(key);
        invoiceRepository.saveAndFlush(invoice);
        for (SaleAllocation allocation : saleAllocations) movementRepository.save(InventoryMovement.builder().branch(invoice.getBranch()).sku(allocation.sku()).batch(allocation.batch())
                .movementType(InventoryMovementType.SALE).quantityDelta(allocation.quantity().negate()).unitCostSnapshot(allocation.unitCost())
                .referenceType("INVOICE").referenceId(invoice.getId()).performedByStaff(staff).idempotencyKey(null).build());
        Purchase firstMembershipPurchase = null;
        for (InvoiceItem item : membershipItems) {
            Purchase purchase = createMembershipPurchase(invoice, item, lockedCustomer, staff, payments.isEmpty() ? null : payments.getFirst());
            item.setPurchase(purchase);
            if (firstMembershipPurchase == null) firstMembershipPurchase = purchase;
        }
        for (PaymentRequest payment : payments) invoice.getPayments().add(InvoicePayment.builder().invoice(invoice).branch(invoice.getBranch())
                .paymentType("PAYMENT").status("COMPLETED").paymentMode(payment.getPaymentMode()).amount(payment.getAmount()).providerReference(blankToNull(payment.getProviderReference()))
                .receivedByStaff(staff).paidAt(LocalDateTime.now()).build());
        if (Boolean.TRUE.equals(request.getCompleteCheckout())) {
            if (invoice.getCheckin() == null) throw new BusinessRuleException("CHECKIN_REQUIRED: A check-in is required to complete checkout");
            com.playville.crm.dto.checkin.CheckoutRequest checkout = new com.playville.crm.dto.checkin.CheckoutRequest();
            checkout.setCheckoutNotes(request.getCheckoutNotes());
            checkout.setConversionOutcome(firstMembershipPurchase == null ? ConversionOutcome.NOT_OFFERED : ConversionOutcome.PURCHASED);
            checkout.setPurchaseId(firstMembershipPurchase == null ? null : firstMembershipPurchase.getId());
            checkinService.checkout(invoice.getCheckin().getId(), checkout, username);
        }
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse recordPayment(Integer invoiceId, PaymentRequest request, String username, String idempotencyKey) {
        String key = blankToNull(idempotencyKey);
        if (key == null) throw new BusinessRuleException("IDEMPOTENCY_KEY_REQUIRED: Idempotency-Key header is required to record a payment");
        Optional<InvoicePayment> previous = paymentRepository.findByIdempotencyKey(key);
        if (previous.isPresent()) {
            Invoice existing = previous.get().getInvoice();
            enforceBranch(existing);
            if (!existing.getId().equals(invoiceId)) throw new ApiConflictException("IDEMPOTENCY_KEY_REUSED", "Idempotency-Key was already used for another invoice payment");
            return toResponse(existing);
        }
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        enforceBranch(invoice);
        if (invoice.getStatus() == InvoiceStatus.DRAFT || invoice.getStatus() == InvoiceStatus.VOIDED || invoice.getStatus() == InvoiceStatus.REFUNDED)
            throw new BusinessRuleException("INVOICE_PAYMENT_NOT_ALLOWED: Payments can only be added to an issued invoice");
        BigDecimal amount = money(request.getAmount());
        if (amount.signum() <= 0 || amount.compareTo(invoice.getBalanceDue()) > 0)
            throw new BusinessRuleException("PAYMENT_EXCEEDS_BALANCE: Payment must be greater than zero and cannot exceed the pending balance");
        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username).orElse(null);
        invoice.getPayments().add(InvoicePayment.builder().invoice(invoice).branch(invoice.getBranch()).paymentType("PAYMENT").status("COMPLETED")
                .paymentMode(request.getPaymentMode()).amount(amount).providerReference(blankToNull(request.getProviderReference()))
                .receivedByStaff(staff).paidAt(LocalDateTime.now()).idempotencyKey(key).build());
        invoice.setAmountPaid(money(invoice.getAmountPaid().add(amount)));
        invoice.setBalanceDue(money(invoice.getGrandTotal().subtract(invoice.getAmountPaid())));
        invoice.setStatus(paymentStatus(invoice.getBalanceDue(), invoice.getGrandTotal()));
        return toResponse(invoiceRepository.saveAndFlush(invoice));
    }

    @Transactional(readOnly = true)
    public byte[] document(Integer invoiceId) {
        Invoice invoice = invoiceRepository.findDetailById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        enforceBranch(invoice);
        String html = invoiceDocumentHtml(invoice);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new PdfRendererBuilder().useFastMode().withHtmlContent(html, null).toStream(output).run();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate invoice PDF", exception);
        }
    }

    private String invoiceDocumentHtml(Invoice invoice) {
        Branch branch = invoice.getBranch();
        Customer customer = invoice.getCustomer();
        String sellerName = firstNonBlank(invoice.getSellerLegalNameSnapshot(), branch.getInvoiceLegalName(), branch.getBranchName());
        String sellerAddress = firstNonBlank(invoice.getSellerAddressSnapshot(), branch.getAddress());
        String sellerPhone = firstNonBlank(invoice.getSellerPhoneSnapshot(), branch.getPhone());
        String sellerEmail = firstNonBlank(invoice.getSellerEmailSnapshot(), branch.getNotificationEmail());
        String gstin = firstNonBlank(invoice.getSellerGstinSnapshot(), branch.getGstin());
        String pan = firstNonBlank(invoice.getSellerPanSnapshot(), branch.getPanNumber());
        String taxState = firstNonBlank(invoice.getSellerTaxStateSnapshot(), branch.getTaxState());
        String taxStateCode = firstNonBlank(invoice.getSellerTaxStateCodeSnapshot(), branch.getTaxStateCode());
        String terms = firstNonBlank(invoice.getTermsSnapshot(), branch.getInvoiceTerms());
        String footer = firstNonBlank(invoice.getFooterSnapshot(), branch.getInvoiceFooter());
        String customerName = firstNonBlank(invoice.getCustomerNameSnapshot(), customer == null ? null : customer.getParentName(), "Walk-in customer");
        String customerPhone = firstNonBlank(invoice.getCustomerPhoneSnapshot(), customer == null ? null : customer.getPhoneNumber());
        String customerEmail = firstNonBlank(invoice.getCustomerEmailSnapshot(), customer == null ? null : customer.getEmail());
        String salesRep = invoice.getIssuedByStaff() == null ? null : invoice.getIssuedByStaff().getFullName();
        boolean taxInvoice = gstin != null;
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>")
                .append("@page{size:A4;margin:14mm 15mm}*{box-sizing:border-box}body{font-family:Arial,sans-serif;color:#111;font-size:10px;margin:0}.brand{font-size:25px;font-weight:800;color:#3b4d9a}.top{width:100%;border-bottom:1px dashed #c7cbd1;padding-bottom:10px;table-layout:fixed}.top td{vertical-align:top;width:33.33%}.right{text-align:right}.center{text-align:center}.title{font-size:18px;font-weight:800;text-transform:none;letter-spacing:.3px}.status{display:inline-block;padding:4px 9px;border-radius:12px;background:#e8f7ef;color:#16794b;font-weight:700}.meta{width:100%;margin:0 0 16px;border-collapse:collapse;border-bottom:2px solid #222}.meta td{width:50%;vertical-align:top;padding:7px 0}.label{font-size:8px;text-transform:uppercase;letter-spacing:.7px;color:#737b88;margin-bottom:4px}.value{font-size:11px;font-weight:700;margin-bottom:5px}.detail{font-weight:700;margin:5px 0}.items{width:100%;border-collapse:collapse;margin-top:12px}.items th{background:#334155;color:white;padding:8px 6px;text-align:left}.items td{border-bottom:1px solid #dfe3ea;padding:8px 6px;vertical-align:top}.num{text-align:right}.muted{color:#737b88;font-size:9px}.totals{width:42%;margin-left:auto;margin-top:14px;border-collapse:collapse}.totals td{padding:5px;border-bottom:1px solid #e5e7eb}.grand td{font-size:13px;font-weight:800;border-top:2px solid #334155}.payments{width:100%;border-collapse:collapse;margin-top:10px}.payments th,.payments td{padding:6px;border-bottom:1px solid #e5e7eb;text-align:left}.section{margin-top:18px;font-size:12px;font-weight:700;color:#334155}.note{margin-top:18px;padding:10px;background:#f4f6fb;border-left:3px solid #6675d8;white-space:pre-line}.footer{text-align:center;color:#737b88;border-top:1px solid #dfe3ea;margin-top:22px;padding-top:10px}")
                .append("</style></head><body><table class=\"top\"><tr><td><div class=\"brand\">PlayVille</div><div class=\"value\">")
                .append(escape(sellerName)).append("</div></td><td class=\"center\"><div class=\"title\">").append(taxInvoice ? "Tax Invoice" : "Invoice").append("</div></td><td class=\"right\"><div class=\"detail\">").append(escape(sellerAddress)).append("</div>");
        if (sellerPhone != null) html.append("<div class=\"muted\">Phone: ").append(escape(sellerPhone)).append("</div>");
        if (sellerEmail != null) html.append("<div class=\"muted\">Email: ").append(escape(sellerEmail)).append("</div>");
        if (taxInvoice) html.append("<div class=\"detail\">GSTIN No : ").append(escape(gstin)).append("</div>");
        if (pan != null) html.append("<div class=\"muted\">PAN: ").append(escape(pan)).append("</div>");
        if (taxState != null) html.append("<div class=\"muted\">State: ").append(escape(taxState)).append(taxStateCode == null ? "" : " (" + escape(taxStateCode) + ")").append("</div>");
        html.append("</td></tr></table><table class=\"meta\"><tr><td><div class=\"detail\">Customer Name : ").append(escape(customerName)).append("</div>");
        if (customerEmail != null) html.append("<div class=\"detail\">Email : ").append(escape(customerEmail)).append("</div>");
        if (customerPhone != null) html.append("<div class=\"detail\">Mobile : ").append(escape(customerPhone)).append("</div>");
        html.append("<div class=\"detail\">Place of Supply : ").append(escape(firstNonBlank(taxState, branch.getCity(), "-"))).append("</div></td><td class=\"right\"><div class=\"detail\">Invoice Type : ").append(escape(invoice.getInvoiceType())).append("</div><div class=\"detail\">Invoice No. : ").append(escape(invoice.getInvoiceNumber() == null ? "Draft #" + invoice.getId() : invoice.getInvoiceNumber())).append("</div><div class=\"detail\">Date of Invoice : ").append(invoice.getInvoiceDate() == null ? "Draft" : invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))).append("</div>");
        if (salesRep != null) html.append("<div class=\"detail\">Sales Rep : ").append(escape(salesRep)).append("</div>");
        html.append("</td></tr></table>");
        html.append("<table class=\"items\"><thead><tr><th>#</th><th>Description</th><th>Item Code</th><th class=\"num\">Qty</th><th class=\"num\">Rate</th><th class=\"num\">Taxable</th>");
        if (taxInvoice) html.append("<th class=\"num\">GST</th>");
        html.append("<th class=\"num\">Amount</th></tr></thead><tbody>");
        for (InvoiceItem item : invoice.getItems()) {
            String taxCode = item.getSku() != null && item.getSku().getTaxProfile() != null ? item.getSku().getTaxProfile().getTaxCode() : item.getSkuSnapshot();
            html.append("<tr><td>").append(item.getLineNumber()).append("</td><td><strong>").append(escape(item.getDescriptionSnapshot())).append("</strong><div class=\"muted\">").append(escape(item.getLineType().name())).append("</div></td><td>")
                    .append(escape(nullToBlank(taxCode))).append("</td><td class=\"num\">").append(item.getQuantity()).append("</td><td class=\"num\">").append(moneyText(item.getUnitPrice())).append("</td><td class=\"num\">").append(moneyText(item.getTaxableAmount())).append("</td>");
            if (taxInvoice) html.append("<td class=\"num\">").append(moneyText(item.getTaxAmount())).append("<div class=\"muted\">").append(item.getTaxRateSnapshot()).append("%</div></td>");
            html.append("<td class=\"num\"><strong>").append(moneyText(item.getLineTotal())).append("</strong></td></tr>");
        }
        html.append("</tbody></table><table class=\"totals\"><tr><td>Subtotal</td><td class=\"num\">").append(moneyText(invoice.getSubtotal())).append("</td></tr>");
        if (invoice.getDiscountTotal().signum() != 0) html.append("<tr><td>Discount</td><td class=\"num\">-").append(moneyText(invoice.getDiscountTotal())).append("</td></tr>");
        if (taxInvoice) { BigDecimal cgst = money(invoice.getTaxTotal().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP)); BigDecimal sgst = money(invoice.getTaxTotal().subtract(cgst)); html.append("<tr><td>CGST</td><td class=\"num\">").append(moneyText(cgst)).append("</td></tr><tr><td>SGST</td><td class=\"num\">").append(moneyText(sgst)).append("</td></tr><tr><td>Total GST</td><td class=\"num\">").append(moneyText(invoice.getTaxTotal())).append("</td></tr>"); }
        if (invoice.getRoundingAdjustment().signum() != 0) html.append("<tr><td>Rounding</td><td class=\"num\">").append(moneyText(invoice.getRoundingAdjustment())).append("</td></tr>");
        html.append("<tr class=\"grand\"><td>Grand total</td><td class=\"num\">").append(moneyText(invoice.getGrandTotal())).append("</td></tr><tr><td>Amount paid</td><td class=\"num\">").append(moneyText(invoice.getAmountPaid())).append("</td></tr><tr><td><strong>Balance due</strong></td><td class=\"num\"><strong>").append(moneyText(invoice.getBalanceDue())).append("</strong></td></tr></table>");
        if (!invoice.getPayments().isEmpty()) {
            html.append("<div class=\"section\">Payment details</div><table class=\"payments\"><thead><tr><th>Date</th><th>Mode</th><th>Reference</th><th>Status</th><th class=\"num\">Amount</th></tr></thead><tbody>");
            for (InvoicePayment payment : invoice.getPayments()) html.append("<tr><td>").append(payment.getPaidAt() == null ? "-" : payment.getPaidAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))).append("</td><td>").append(escape(payment.getPaymentMode().name())).append("</td><td>").append(escape(firstNonBlank(payment.getProviderReference(), "-"))).append("</td><td>").append(escape(payment.getStatus())).append("</td><td class=\"num\">").append(moneyText(payment.getAmount())).append("</td></tr>");
            html.append("</tbody></table>");
        }
        if (invoice.getNotes() != null) html.append("<div class=\"note\"><strong>Notes</strong><br/>").append(escape(invoice.getNotes())).append("</div>");
        if (terms != null) html.append("<div class=\"note\"><strong>Terms and conditions</strong><br/>").append(escape(terms)).append("</div>");
        html.append("<div class=\"footer\">").append(escape(firstNonBlank(footer, "Thank you for choosing PlayVille."))).append("<br/>This is a computer-generated invoice and does not require a signature.</div></body></html>");
        return html.toString();
    }

    @Transactional
    public InvoiceResponse voidDraft(Integer invoiceId, VoidInvoiceRequest request, String username) {
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        enforceBranch(invoice);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) throw new BusinessRuleException("INVOICE_VOID_NOT_ALLOWED: Issued invoices must be corrected through a return/credit note");
        invoice.setStatus(InvoiceStatus.VOIDED); invoice.setVoidReason(request.getReason().trim()); invoice.setVoidedByStaff(staffRepository.findByUsernameAndIsActiveTrue(username).orElse(null));
        return toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse returnItems(Integer invoiceId, CreateReturnRequest request, String username, String idempotencyKey) {
        String key = blankToNull(idempotencyKey);
        if (key == null) throw new BusinessRuleException("IDEMPOTENCY_KEY_REQUIRED: Idempotency-Key header is required to return an invoice");
        var existing = salesReturnRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            SalesReturn previous = existing.get();
            if (!previous.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
            if (!previous.getOriginalInvoice().getId().equals(invoiceId)) {
                throw new ApiConflictException("IDEMPOTENCY_KEY_REUSED", "Idempotency-Key was already used for a different invoice return");
            }
            return toResponse(previous.getCreditInvoice());
        }
        Invoice original = invoiceRepository.findByIdForUpdate(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        enforceBranch(original);
        Optional<SalesReturn> returnRecordedWhileWaiting = salesReturnRepository.findByIdempotencyKey(key);
        if (returnRecordedWhileWaiting.isPresent()) {
            SalesReturn previous = returnRecordedWhileWaiting.get();
            if (!previous.getOriginalInvoice().getId().equals(invoiceId)) {
                throw new ApiConflictException("IDEMPOTENCY_KEY_REUSED", "Idempotency-Key was already used for a different invoice return");
            }
            return toResponse(previous.getCreditInvoice());
        }
        if (original.getStatus() != InvoiceStatus.PAID && original.getStatus() != InvoiceStatus.PARTIALLY_REFUNDED) throw new BusinessRuleException("INVOICE_RETURN_NOT_ALLOWED: Only paid invoices can be returned");
        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username).orElse(null);
        Invoice credit = Invoice.builder().branch(original.getBranch()).customer(original.getCustomer()).invoiceType("CREDIT_NOTE").status(InvoiceStatus.REFUNDED)
                .invoiceNumber(nextInvoiceNumber(original.getBranch())).invoiceDate(LocalDateTime.now()).customerNameSnapshot(original.getCustomerNameSnapshot())
                .customerPhoneSnapshot(original.getCustomerPhoneSnapshot()).customerEmailSnapshot(original.getCustomerEmailSnapshot()).notes(request.getNotes()).issuedByStaff(staff).build();
        invoiceRepository.saveAndFlush(credit);
        SalesReturn salesReturn = salesReturnRepository.saveAndFlush(SalesReturn.builder().originalInvoice(original).creditInvoice(credit).branch(original.getBranch())
                .reason(request.getReason().trim()).notes(blankToNull(request.getNotes())).returnedByStaff(staff).idempotencyKey(key).build());
        BigDecimal refundTotal = BigDecimal.ZERO;
        BigDecimal refundGrossTotal = BigDecimal.ZERO;
        int lineNo = 1;
        for (ReturnInvoiceLineRequest returnLine : request.getItems()) {
            InvoiceItem source = original.getItems().stream().filter(item -> item.getId().equals(returnLine.getInvoiceItemId())).findFirst()
                    .orElseThrow(() -> new BusinessRuleException("RETURN_LINE_NOT_FOUND: Invoice line does not belong to this invoice"));
            if (source.getLineType() != InvoiceLineType.RETAIL) throw new BusinessRuleException("MEMBERSHIP_RETURN_NOT_ALLOWED: Only retail lines are returnable");
            if (source.getReturnedQuantity().add(returnLine.getQuantity()).compareTo(source.getQuantity()) > 0) throw new ApiConflictException("RETURN_QUANTITY_EXCEEDED", "Return quantity exceeds the unreturned quantity");
            boolean resellable = Boolean.TRUE.equals(returnLine.getResellable());
            if (resellable && source.getSku().isHasExpiry()) throw new BusinessRuleException("EXPIRY_ITEM_RETURN_REQUIRES_INSPECTION: Expiry-controlled items cannot be automatically returned to sellable stock");
            BigDecimal ratio = returnLine.getQuantity().divide(source.getQuantity(), 8, RoundingMode.HALF_UP);
            BigDecimal gross = source.getGrossAmount().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxable = source.getTaxableAmount().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tax = source.getTaxAmount().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = source.getLineTotal().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            credit.getItems().add(InvoiceItem.builder().invoice(credit).lineNumber(lineNo++).lineType(InvoiceLineType.RETAIL).product(source.getProduct()).sku(source.getSku())
                    .descriptionSnapshot(source.getDescriptionSnapshot()).skuSnapshot(source.getSkuSnapshot()).quantity(returnLine.getQuantity()).unitPrice(source.getUnitPrice())
                    .grossAmount(gross).discountAmount(source.getDiscountAmount().multiply(ratio).setScale(2, RoundingMode.HALF_UP))
                    .taxableAmount(taxable).taxRateSnapshot(source.getTaxRateSnapshot()).taxAmount(tax).lineTotal(total).build());
            salesReturnItemRepository.save(SalesReturnItem.builder().salesReturn(salesReturn).originalInvoiceItem(source).quantity(returnLine.getQuantity()).resellable(resellable).build());
            source.setReturnedQuantity(source.getReturnedQuantity().add(returnLine.getQuantity()));
            if (resellable) restockReturn(original, source, returnLine.getQuantity(), staff);
            refundTotal = refundTotal.add(total);
            refundGrossTotal = refundGrossTotal.add(gross);
        }
        credit.setSubtotal(refundGrossTotal); credit.setTaxTotal(credit.getItems().stream().map(InvoiceItem::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add)); credit.setGrandTotal(refundTotal); credit.setAmountPaid(refundTotal); credit.setBalanceDue(BigDecimal.ZERO);
        credit.getPayments().add(InvoicePayment.builder().invoice(credit).branch(credit.getBranch()).paymentType("REFUND").paymentMode(request.getRefundMode()).amount(refundTotal).receivedByStaff(staff).paidAt(LocalDateTime.now()).notes("Refund for " + original.getInvoiceNumber()).build());
        boolean fullyReturned = original.getItems().stream().allMatch(item -> item.getLineType() == InvoiceLineType.RETAIL
                && item.getReturnedQuantity().compareTo(item.getQuantity()) == 0);
        original.setStatus(fullyReturned ? InvoiceStatus.REFUNDED : InvoiceStatus.PARTIALLY_REFUNDED);
        invoiceRepository.save(original); invoiceRepository.save(credit);
        return toResponse(credit);
    }

    private void replaceLines(Invoice invoice, List<InvoiceLineRequest> requests) {
        invoice.getItems().clear();
        int lineNumber = 1;
        for (InvoiceLineRequest request : requests) invoice.getItems().add(buildLine(invoice, lineNumber++, request));
        recalculate(invoice);
        invoiceRepository.saveAndFlush(invoice);
    }

    private void claimVisitItemReservations(Invoice invoice, List<InvoiceItem> retailItems) {
        if (invoice.getCheckin() == null) throw new BusinessRuleException("CHECKIN_REQUIRED: A check-in is required to complete checkout");
        List<CheckinSessionItem> visitItems = sessionItemRepository.findByCheckinIdAndStatusOrderByCreatedAtAsc(
                invoice.getCheckin().getId(), CheckinSessionItem.Status.OPEN);
        Map<Integer, BigDecimal> expected = new HashMap<>();
        visitItems.forEach(item -> expected.merge(item.getSku().getId(), item.getQuantity(), BigDecimal::add));
        Map<Integer, BigDecimal> invoiced = new HashMap<>();
        retailItems.forEach(item -> invoiced.merge(item.getSku().getId(), item.getQuantity(), BigDecimal::add));
        if (!expected.equals(invoiced))
            throw new BusinessRuleException("VISIT_INVOICE_MISMATCH: Refresh the checkout invoice because visit items have changed");
        for (CheckinSessionItem item : visitItems) {
            if (!item.getSku().getProduct().isTrackInventory()) {
                item.setStatus(CheckinSessionItem.Status.CHARGED);
                sessionItemRepository.save(item);
                continue;
            }
            InventoryBalance balance = balanceRepository.findByBranchIdAndSkuIdForUpdate(invoice.getBranch().getId(), item.getSku().getId())
                    .orElseThrow(() -> new ApiConflictException("INVENTORY_RESERVATION_MISMATCH", "Inventory balance is missing for " + item.getSkuCodeSnapshot()));
            if (balance.getQuantityReserved().compareTo(item.getQuantity()) < 0)
                throw new ApiConflictException("INVENTORY_RESERVATION_MISMATCH", "Reserved stock is no longer available for " + item.getProductNameSnapshot());
            balance.setQuantityReserved(balance.getQuantityReserved().subtract(item.getQuantity()));
            balanceRepository.save(balance);
            item.setStatus(CheckinSessionItem.Status.CHARGED);
            sessionItemRepository.save(item);
        }
    }

    private void enforceCheckinBranch(Checkin checkin) {
        if (!checkin.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
    }

    private InvoiceItem buildLine(Invoice invoice, int lineNumber, InvoiceLineRequest request) {
        if (request.getQuantity() == null || request.getQuantity().signum() <= 0)
            throw new BusinessRuleException("INVALID_QUANTITY: Quantity must be greater than zero");
        if (request.getLineType() == InvoiceLineType.MEMBERSHIP) {
            if (request.getPackageId() == null) throw new BusinessRuleException("PACKAGE_REQUIRED: Package is required for a membership line");
            if (invoice.getCustomer() == null) throw new BusinessRuleException("CUSTOMER_REQUIRED: A customer is required for membership purchases");
            PlayvillePackage pkg = packageRepository.findById(request.getPackageId()).orElseThrow(() -> new ResourceNotFoundException("Package", request.getPackageId()));
            if (!pkg.isActive()) throw new ApiConflictException("PACKAGE_INACTIVE", "Package is no longer available");
            if (request.getQuantity().compareTo(BigDecimal.ONE) != 0) throw new BusinessRuleException("INVALID_MEMBERSHIP_QUANTITY: Membership quantity must be one");
            BigDecimal price = pkg.getTotalPrice();
            return InvoiceItem.builder().invoice(invoice).lineNumber(lineNumber).lineType(InvoiceLineType.MEMBERSHIP).playvillePackage(pkg)
                    .descriptionSnapshot(pkg.getPackageName()).quantity(BigDecimal.ONE).unitPrice(price).grossAmount(price).taxableAmount(price)
                    .taxRateSnapshot(BigDecimal.ZERO).taxAmount(BigDecimal.ZERO).lineTotal(price).build();
        }
        if (request.getLineType() == InvoiceLineType.CHECKOUT_CHARGE)
            throw new BusinessRuleException("LINE_TYPE_NOT_AVAILABLE: Checkout-charge lines must be finalized through the check-in checkout flow");
        if (request.getSkuId() == null) throw new BusinessRuleException("SKU_REQUIRED: SKU is required for this invoice line");
        ProductSku sku = skuRepository.findById(request.getSkuId()).orElseThrow(() -> new ResourceNotFoundException("SKU", request.getSkuId()));
        Product product = sku.getProduct();
        if (!product.isActive() || !sku.isActive()) throw new ApiConflictException("PRODUCT_INACTIVE", "Product is no longer available for sale");
        if (request.getLineType() == InvoiceLineType.RETAIL && product.getProductType() != ProductType.RETAIL)
            throw new BusinessRuleException("INVALID_INVOICE_LINE: Retail lines require a retail SKU");
        if (request.getLineType() == InvoiceLineType.SERVICE && product.getProductType() != ProductType.SERVICE)
            throw new BusinessRuleException("INVALID_INVOICE_LINE: Service lines require a service SKU");
        if (request.getLineType() == InvoiceLineType.RETAIL && product.isTrackInventory()) {
            BranchSku config = branchSkuRepository.findByBranchIdAndSkuId(invoice.getBranch().getId(), sku.getId()).orElse(null);
            InventoryBalance balance = balanceRepository.findByBranchIdAndSkuId(invoice.getBranch().getId(), sku.getId()).orElse(null);
            BigDecimal visitReservation = invoice.getCheckin() == null ? BigDecimal.ZERO : sessionItemRepository
                    .findByCheckinIdAndSkuIdAndStatus(invoice.getCheckin().getId(), sku.getId(), CheckinSessionItem.Status.OPEN)
                    .map(CheckinSessionItem::getQuantity).orElse(BigDecimal.ZERO);
            if (config != null && !config.isAllowNegativeStock() && (balance == null || balance.availableQuantity().add(visitReservation).compareTo(request.getQuantity()) < 0)) {
                throw new ApiConflictException("INSUFFICIENT_STOCK", "Insufficient stock for " + sku.getSkuCode());
            }
        }
        BranchSku branchSku = branchSkuRepository.findByBranchIdAndSkuId(invoice.getBranch().getId(), sku.getId()).orElse(null);
        if (branchSku == null || !branchSku.isAvailable()) throw new ApiConflictException("SKU_NOT_AVAILABLE_AT_BRANCH", "SKU is not available at this branch");
        BigDecimal unitPrice = branchSku.getSalePriceOverride() == null ? sku.getDefaultSalePrice() : branchSku.getSalePriceOverride();
        BigDecimal gross = unitPrice.multiply(request.getQuantity()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal rate = sku.getTaxProfile() == null ? BigDecimal.ZERO : sku.getTaxProfile().getRatePercent();
        boolean taxInclusive = sku.getTaxProfile() == null || sku.getTaxProfile().isPriceIncludesTax();
        BigDecimal taxable = taxInclusive && rate.signum() > 0 ? gross.divide(BigDecimal.ONE.add(rate.movePointLeft(2)), 2, RoundingMode.HALF_UP) : gross;
        BigDecimal tax = taxInclusive ? gross.subtract(taxable) : taxable.multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = taxInclusive ? gross : gross.add(tax);
        return InvoiceItem.builder().invoice(invoice).lineNumber(lineNumber).lineType(request.getLineType()).product(product).sku(sku)
                .descriptionSnapshot(product.getProductName()).skuSnapshot(sku.getSkuCode()).quantity(request.getQuantity()).unitPrice(unitPrice)
                .grossAmount(gross).taxableAmount(taxable).taxRateSnapshot(rate).taxAmount(tax).lineTotal(total).build();
    }

    private void recalculate(Invoice invoice) { BigDecimal subtotal = invoice.getItems().stream().map(InvoiceItem::getGrossAmount).reduce(BigDecimal.ZERO, BigDecimal::add); BigDecimal tax = invoice.getItems().stream().map(InvoiceItem::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add); BigDecimal total = invoice.getItems().stream().map(InvoiceItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add); invoice.setSubtotal(subtotal); invoice.setTaxTotal(tax); invoice.setGrandTotal(total); invoice.setBalanceDue(total); }
    private List<SaleAllocation> allocateStock(Invoice invoice, InvoiceItem item) {
        ProductSku sku = item.getSku(); BigDecimal remaining = item.getQuantity(); List<SaleAllocation> allocations = new ArrayList<>();
        List<InventoryBatch> batches = batchRepository.findSellableForUpdate(invoice.getBranch().getId(), sku.getId());
        for (InventoryBatch batch : batches) { if (remaining.signum() <= 0) break; BigDecimal allocated = batch.getQuantityRemaining().min(remaining); batch.setQuantityRemaining(batch.getQuantityRemaining().subtract(allocated)); batchRepository.save(batch); allocations.add(new SaleAllocation(sku, batch, allocated, batch.getUnitCost())); remaining = remaining.subtract(allocated); }
        if (remaining.signum() > 0) { if (sku.isHasExpiry()) throw new ApiConflictException("BATCH_EXPIRED", "No valid unexpired batch is available for " + sku.getSkuCode()); allocations.add(new SaleAllocation(sku, null, remaining, sku.getDefaultCostPrice())); }
        return allocations;
    }
    private Purchase createMembershipPurchase(Invoice invoice, InvoiceItem item, Customer customer, Staff staff, PaymentRequest payment) {
        PlayvillePackage pkg = item.getPlayvillePackage(); int before = customer.getGlobalSessionBalance(); int after = before + pkg.getSessionsTotal();
        Checkin checkin = invoice.getCheckin();
        Purchase purchase = Purchase.builder().customer(customer).branch(invoice.getBranch()).staff(staff).playvillePackage(pkg).sessionsAdded(pkg.getSessionsTotal())
                .amountPaid(item.getLineTotal()).gstAmount(item.getTaxAmount()).discountApplied(BigDecimal.ZERO).balanceBefore(before).balanceAfter(after)
                .paymentMode(payment == null ? Purchase.PaymentMode.Cash : toPurchasePaymentMode(payment.getPaymentMode())).paymentReference(payment == null ? null : payment.getProviderReference()).notes("Invoice " + invoice.getInvoiceNumber())
                .sourceCheckin(checkin).sourceTrialEntitlement(checkin == null ? null : checkin.getEntitlement())
                .purchaseContext(checkin != null && "COMPLIMENTARY_TRIAL".equals(checkin.getVisitType()) ? PurchaseContext.TRIAL_CHECKOUT : PurchaseContext.STANDARD).build();
        purchaseRepository.saveAndFlush(purchase); customer.setGlobalSessionBalance(after); customer.setCurrentPackage(pkg); customer.setPurchaseBranch(invoice.getBranch()); customerRepository.save(customer); return purchase;
    }
    private void restockReturn(Invoice original, InvoiceItem source, BigDecimal quantity, Staff staff) {
        InventoryBalance balance = balanceRepository.findByBranchIdAndSkuIdForUpdate(original.getBranch().getId(), source.getSku().getId())
                .orElseGet(() -> balanceRepository.saveAndFlush(InventoryBalance.builder().branch(original.getBranch()).sku(source.getSku()).build()));
        balance.setQuantityOnHand(balance.getQuantityOnHand().add(quantity)); balanceRepository.save(balance);
        movementRepository.save(InventoryMovement.builder().branch(original.getBranch()).sku(source.getSku()).movementType(InventoryMovementType.SALE_RETURN).quantityDelta(quantity)
                .unitCostSnapshot(source.getSku().getDefaultCostPrice()).referenceType("CREDIT_NOTE").referenceId(original.getId()).performedByStaff(staff).build());
    }
    private Purchase.PaymentMode toPurchasePaymentMode(InvoicePaymentMode mode) { return switch (mode) { case CASH -> Purchase.PaymentMode.Cash; case UPI -> Purchase.PaymentMode.UPI; case CARD -> Purchase.PaymentMode.Card; case ONLINE -> Purchase.PaymentMode.Online; }; }
    private InvoicePaymentMode toInvoicePaymentMode(Purchase.PaymentMode mode) { return switch (mode == null ? Purchase.PaymentMode.Cash : mode) { case Cash -> InvoicePaymentMode.CASH; case UPI -> InvoicePaymentMode.UPI; case Card -> InvoicePaymentMode.CARD; case Online -> InvoicePaymentMode.ONLINE; }; }
    private BigDecimal money(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP); }
    private InvoiceStatus paymentStatus(BigDecimal balanceDue, BigDecimal grandTotal) {
        if (balanceDue.signum() == 0) return InvoiceStatus.PAID;
        return balanceDue.compareTo(grandTotal) == 0 ? InvoiceStatus.UNPAID : InvoiceStatus.PARTIALLY_PAID;
    }
    private void captureCustomerSnapshot(Invoice invoice, Customer customer) { if (customer != null) { invoice.setCustomerNameSnapshot(customer.getParentName()); invoice.setCustomerPhoneSnapshot(customer.getPhoneNumber()); invoice.setCustomerEmailSnapshot(customer.getEmail()); } }
    public void captureSellerSnapshot(Invoice invoice, Branch branch) {
        invoice.setSellerLegalNameSnapshot(firstNonBlank(branch.getInvoiceLegalName(), branch.getBranchName()));
        invoice.setSellerAddressSnapshot(blankToNull(branch.getAddress()));
        invoice.setSellerPhoneSnapshot(blankToNull(branch.getPhone()));
        invoice.setSellerEmailSnapshot(blankToNull(branch.getNotificationEmail()));
        invoice.setSellerGstinSnapshot(blankToNull(branch.getGstin()));
        invoice.setSellerPanSnapshot(blankToNull(branch.getPanNumber()));
        invoice.setSellerTaxStateSnapshot(blankToNull(branch.getTaxState()));
        invoice.setSellerTaxStateCodeSnapshot(blankToNull(branch.getTaxStateCode()));
        invoice.setTermsSnapshot(blankToNull(branch.getInvoiceTerms()));
        invoice.setFooterSnapshot(blankToNull(branch.getInvoiceFooter()));
    }
    public void prepareIssuedInvoice(Invoice invoice) {
        captureSellerSnapshot(invoice, invoice.getBranch());
        if (invoice.getInvoiceNumber() == null) invoice.setInvoiceNumber(nextInvoiceNumber(invoice.getBranch()));
        if (invoice.getInvoiceDate() == null) invoice.setInvoiceDate(LocalDateTime.now());
    }
    private void requireDraftAndVersion(Invoice invoice, Long version) { if (invoice.getStatus() != InvoiceStatus.DRAFT) throw new ApiConflictException("INVOICE_NOT_EDITABLE", "Only a draft invoice can be changed"); if (!Objects.equals(invoice.getVersion(), version)) throw new ApiConflictException("STALE_INVOICE_VERSION", "Invoice has changed. Refresh and review the latest draft"); }
    private void enforceBranch(Invoice invoice) { if (!invoice.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException(); }
    private String nextInvoiceNumber(Branch branch) { LocalDate today = LocalDate.now(java.time.ZoneId.of(firstNonBlank(branch.getTimezone(), "Asia/Kolkata"))); int start = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1; String fy = start + "-" + String.format("%02d", (start + 1) % 100); InvoiceSequence sequence = sequenceRepository.findForUpdate(branch.getId(), fy).orElseGet(() -> sequenceRepository.saveAndFlush(InvoiceSequence.builder().branch(branch).financialYear(fy).nextNumber(1).build())); int value = sequence.getNextNumber(); sequence.setNextNumber(value + 1); return firstNonBlank(branch.getInvoicePrefix(), branch.getBranchCode()) + "/" + fy + "/" + String.format("%06d", value); }
    private InvoiceResponse toResponse(Invoice i) {
        Integer purchaseId = i.getItems().stream().map(InvoiceItem::getPurchase).filter(Objects::nonNull).map(Purchase::getId).findFirst().orElse(null);
        Customer customer = i.getCustomer(); Branch branch = i.getBranch();
        return InvoiceResponse.builder().id(i.getId()).invoiceNumber(i.getInvoiceNumber()).branchId(i.getBranch().getId()).branchCode(i.getBranch().getBranchCode())
                .customerId(i.getCustomer() == null ? null : i.getCustomer().getId()).checkinId(i.getCheckin() == null ? null : i.getCheckin().getId()).birthdayBookingId(i.getBirthdayBooking() == null ? null : i.getBirthdayBooking().getId())
                .status(i.getStatus()).invoiceType(i.getInvoiceType()).invoiceDate(i.getInvoiceDate()).purchaseId(purchaseId)
                .issuedByStaffName(i.getIssuedByStaff() == null ? null : i.getIssuedByStaff().getFullName())
                .sellerLegalName(firstNonBlank(i.getSellerLegalNameSnapshot(), branch.getInvoiceLegalName(), branch.getBranchName()))
                .sellerAddress(firstNonBlank(i.getSellerAddressSnapshot(), branch.getAddress())).sellerPhone(firstNonBlank(i.getSellerPhoneSnapshot(), branch.getPhone()))
                .sellerEmail(firstNonBlank(i.getSellerEmailSnapshot(), branch.getNotificationEmail())).sellerGstin(firstNonBlank(i.getSellerGstinSnapshot(), branch.getGstin()))
                .sellerPan(firstNonBlank(i.getSellerPanSnapshot(), branch.getPanNumber())).sellerTaxState(firstNonBlank(i.getSellerTaxStateSnapshot(), branch.getTaxState()))
                .sellerTaxStateCode(firstNonBlank(i.getSellerTaxStateCodeSnapshot(), branch.getTaxStateCode()))
                .terms(firstNonBlank(i.getTermsSnapshot(), branch.getInvoiceTerms())).footer(firstNonBlank(i.getFooterSnapshot(), branch.getInvoiceFooter())).version(i.getVersion())
                .customerName(firstNonBlank(i.getCustomerNameSnapshot(), customer == null ? null : customer.getParentName()))
                .customerPhone(firstNonBlank(i.getCustomerPhoneSnapshot(), customer == null ? null : customer.getPhoneNumber()))
                .customerEmail(firstNonBlank(i.getCustomerEmailSnapshot(), customer == null ? null : customer.getEmail()))
                .subtotal(i.getSubtotal()).discountTotal(i.getDiscountTotal()).taxTotal(i.getTaxTotal()).roundingAdjustment(i.getRoundingAdjustment())
                .grandTotal(i.getGrandTotal()).amountPaid(i.getAmountPaid()).balanceDue(i.getBalanceDue()).notes(i.getNotes()).voidReason(i.getVoidReason())
                .items(i.getItems().stream().sorted(Comparator.comparing(InvoiceItem::getLineNumber)).map(this::toLineResponse).toList())
                .payments(i.getPayments().stream().map(p -> PaymentResponse.builder().id(p.getId()).paymentType(p.getPaymentType()).paymentMode(p.getPaymentMode())
                        .amount(p.getAmount()).providerReference(p.getProviderReference()).status(p.getStatus()).paidAt(p.getPaidAt()).build()).toList())
                .createdAt(i.getCreatedAt()).build();
    }

    private InvoiceLineResponse toLineResponse(InvoiceItem item) {
        return InvoiceLineResponse.builder().id(item.getId()).lineNumber(item.getLineNumber()).lineType(item.getLineType())
                .productId(item.getProduct() == null ? null : item.getProduct().getId()).skuId(item.getSku() == null ? null : item.getSku().getId())
                .packageId(item.getPlayvillePackage() == null ? null : item.getPlayvillePackage().getId())
                .purchaseId(item.getPurchase() == null ? null : item.getPurchase().getId())
                .sessionsIncluded(item.getPurchase() == null ? null : item.getPurchase().getSessionsAdded())
                .validityDays(item.getPlayvillePackage() == null ? null : item.getPlayvillePackage().getValidityDays())
                .description(item.getDescriptionSnapshot()).skuCode(item.getSkuSnapshot())
                .quantity(item.getQuantity()).unitPrice(item.getUnitPrice()).grossAmount(item.getGrossAmount()).discountAmount(item.getDiscountAmount())
                .taxableAmount(item.getTaxableAmount()).taxRate(item.getTaxRateSnapshot()).taxAmount(item.getTaxAmount()).lineTotal(item.getLineTotal())
                .returnedQuantity(item.getReturnedQuantity()).availableQuantity(availableQuantity(item)).build();
    }

    private BigDecimal availableQuantity(InvoiceItem item) {
        if (item.getSku() == null || item.getInvoice() == null) return null;
        return balanceRepository.findByBranchIdAndSkuId(item.getInvoice().getBranch().getId(), item.getSku().getId()).map(InventoryBalance::availableQuantity).orElse(BigDecimal.ZERO);
    }

    private void validatePaymentReferences(List<PaymentRequest> payments) {
        Set<String> references = new HashSet<>();
        for (PaymentRequest payment : payments) {
            String reference = blankToNull(payment.getProviderReference());
            if ((payment.getPaymentMode() == InvoicePaymentMode.UPI || payment.getPaymentMode() == InvoicePaymentMode.CARD || payment.getPaymentMode() == InvoicePaymentMode.ONLINE) && reference == null) {
                throw new BusinessRuleException("PAYMENT_REFERENCE_REQUIRED: Provider reference is required for non-cash payments");
            }
            if (reference != null && !references.add(payment.getPaymentMode() + ":" + reference)) {
                throw new ApiConflictException("DUPLICATE_PAYMENT_REFERENCE", "Payment references must be unique within an invoice");
            }
        }
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String nullToBlank(String value) { return value == null ? "" : value; }
    private String firstNonBlank(String... values) { for (String value : values) if (value != null && !value.isBlank()) return value.trim(); return null; }
    private String purchaseLineDescription(Purchase purchase, PlayvillePackage pkg) {
        LocalDate start = (purchase.getCreatedAt() == null ? LocalDateTime.now() : purchase.getCreatedAt()).toLocalDate();
        String validity = pkg.getValidityDays() == null ? "" : ", Valid " + start.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + " to " + start.plusDays(pkg.getValidityDays()).format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        return pkg.getPackageName() + ", Sessions: " + purchase.getSessionsAdded() + validity;
    }
    private String moneyText(BigDecimal value) { return "&#8377;" + money(value).toPlainString(); }
    private record SaleAllocation(ProductSku sku, InventoryBatch batch, BigDecimal quantity, BigDecimal unitCost) { }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
