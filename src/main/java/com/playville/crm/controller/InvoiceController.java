package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.common.PagedResponse;
import com.playville.crm.dto.invoice.*;
import com.playville.crm.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import com.playville.crm.entity.enums.InvoicePaymentMode;
import com.playville.crm.entity.enums.InvoiceStatus;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final com.playville.crm.service.InvoiceNotificationService invoiceNotificationService;
    @GetMapping public ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> list(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) InvoicePaymentMode paymentMode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InvoiceResponse> result = invoiceService.list(from, to, status, customerId, paymentMode, page, size);
        return ResponseEntity.ok(PagedResponse.of(result));
    }
    @PostMapping public ResponseEntity<ApiResponse<InvoiceResponse>> create(@Valid @RequestBody CreateInvoiceRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Invoice draft created", invoiceService.createDraft(request))); }
    @PostMapping("/checkout-draft/{checkinId}") public ResponseEntity<ApiResponse<InvoiceResponse>> checkoutDraft(@PathVariable Integer checkinId) { return ResponseEntity.ok(ApiResponse.success("Checkout invoice ready", invoiceService.ensureCheckoutDraft(checkinId))); }
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<InvoiceResponse>> get(@PathVariable Integer id) { return ResponseEntity.ok(ApiResponse.success(invoiceService.get(id))); }
    @PutMapping("/{id}/items") public ResponseEntity<ApiResponse<InvoiceResponse>> replaceItems(@PathVariable Integer id, @Valid @RequestBody ReplaceInvoiceItemsRequest request) { return ResponseEntity.ok(ApiResponse.success("Invoice draft updated", invoiceService.replaceItems(id, request))); }
    @PostMapping("/{id}/finalize") public ResponseEntity<ApiResponse<InvoiceResponse>> finalizeInvoice(@PathVariable Integer id, @Valid @RequestBody FinalizeInvoiceRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey, @AuthenticationPrincipal String username) { return ResponseEntity.ok(ApiResponse.success("Invoice finalized", invoiceService.finalizeInvoice(id, request, username, idempotencyKey))); }
    @PostMapping("/{id}/payments") public ResponseEntity<ApiResponse<InvoiceResponse>> recordPayment(@PathVariable Integer id, @Valid @RequestBody PaymentRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey, @AuthenticationPrincipal String username) { return ResponseEntity.ok(ApiResponse.success("Invoice payment recorded", invoiceService.recordPayment(id, request, username, idempotencyKey))); }
    @PostMapping("/{id}/void") @PreAuthorize("hasAnyRole('admin','manager')") public ResponseEntity<ApiResponse<InvoiceResponse>> voidInvoice(@PathVariable Integer id, @Valid @RequestBody VoidInvoiceRequest request, @AuthenticationPrincipal String username) { return ResponseEntity.ok(ApiResponse.success("Invoice voided", invoiceService.voidDraft(id, request, username))); }
    @PostMapping("/{id}/returns") @PreAuthorize("hasAnyRole('admin','manager')") public ResponseEntity<ApiResponse<InvoiceResponse>> returnInvoice(@PathVariable Integer id, @Valid @RequestBody CreateReturnRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey, @AuthenticationPrincipal String username) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Return recorded", invoiceService.returnItems(id, request, username, idempotencyKey))); }
    @GetMapping(value = "/{id}/document", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> document(@PathVariable Integer id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=playville-invoice-" + id + ".pdf")
                .body(invoiceService.document(id));
    }
    @PostMapping("/{id}/share")
    public ResponseEntity<ApiResponse<ShareInvoiceResponse>> share(@PathVariable Integer id, @Valid @RequestBody ShareInvoiceRequest request,
            @RequestHeader("Idempotency-Key") @jakarta.validation.constraints.Size(max = 100) String idempotencyKey,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(ApiResponse.success("Invoice shared", invoiceNotificationService.share(id, request, username, idempotencyKey)));
    }
}
