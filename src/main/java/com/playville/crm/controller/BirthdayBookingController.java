package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.birthday.*;
import com.playville.crm.dto.invoice.InvoiceResponse;
import com.playville.crm.dto.invoice.PaymentRequest;
import com.playville.crm.entity.BirthdayBooking;
import com.playville.crm.service.BirthdayBookingService;
import com.playville.crm.service.BirthdayBillingService;
import com.playville.crm.service.BirthdayQuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Birthday Bookings", description = "Birthday party booking management")
@RestController
@RequestMapping("/birthday-bookings")
@RequiredArgsConstructor
public class BirthdayBookingController {

    private final BirthdayBookingService bookingService;
    private final BirthdayBillingService billingService;
    private final BirthdayQuoteService quoteService;

    @Operation(summary = "Create a birthday party booking")
    @PostMapping
    public ResponseEntity<ApiResponse<BirthdayBookingResponse>> create(
            @Valid @RequestBody BirthdayBookingRequest req,
            @AuthenticationPrincipal String username) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Birthday booking created",
                        bookingService.createBooking(req, username)));
    }

    @Operation(summary = "List active birthday packages")
    @GetMapping("/packages")
    public ResponseEntity<ApiResponse<List<BirthdayPackageResponse>>> packages() {
        return ResponseEntity.ok(ApiResponse.success(quoteService.packages()));
    }

    @Operation(summary = "List active birthday booking catalog items")
    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<List<BirthdayCatalogItemResponse>>> catalog() {
        return ResponseEntity.ok(ApiResponse.success(quoteService.catalog()));
    }

    @GetMapping("/catalog/configuration")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<List<BirthdayCatalogItemResponse>>> catalogConfiguration() {
        return ResponseEntity.ok(ApiResponse.success(quoteService.branchCatalog()));
    }

    @PostMapping("/catalog/configuration")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<BirthdayCatalogItemResponse>> createCatalogItem(@Valid @RequestBody BirthdayCatalogItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Birthday item created", quoteService.createCatalogItem(request)));
    }

    @PutMapping("/catalog/configuration/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<BirthdayCatalogItemResponse>> updateCatalogItem(@PathVariable Integer id, @Valid @RequestBody BirthdayCatalogItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Birthday item updated", quoteService.updateCatalogItem(id, request)));
    }

    @PutMapping("/catalog/configuration/{id}/branch")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<BirthdayCatalogItemResponse>> configureBranchItem(@PathVariable Integer id, @Valid @RequestBody BirthdayBranchCatalogRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Branch birthday item updated", quoteService.configureBranchItem(id, request)));
    }

    @Operation(summary = "Calculate a server-side birthday quote preview")
    @PostMapping("/quote-preview")
    public ResponseEntity<ApiResponse<BirthdayQuotePreviewResponse>> preview(@Valid @RequestBody BirthdayQuotePreviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(quoteService.preview(request)));
    }

    @Operation(summary = "Check whether the standard birthday slot is available")
    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<java.util.Map<String, Boolean>>> availability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate partyDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) java.time.LocalTime partySlotStart) {
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of(
                "available", bookingService.isSlotAvailable(partyDate, partySlotStart))));
    }

    @Operation(summary = "List birthday bookings for current branch")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BirthdayBookingResponse>>> getAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                ApiResponse.success(bookingService.getBranchBookings(from, to)));
    }

    @Operation(summary = "Get birthday booking by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BirthdayBookingResponse>> getById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(bookingService.getBookingById(id)));
    }

    @Operation(summary = "Update booking status")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BirthdayBookingResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestParam BirthdayBooking.BookingStatus status) {
        return ResponseEntity.ok(
                ApiResponse.success(bookingService.updateStatus(id, status)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<BirthdayBookingResponse>> complete(@PathVariable Integer id,
                                                                           @Valid @RequestBody BirthdayCompletionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Birthday completion recorded",
                bookingService.complete(id, request)));
    }

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<ApiResponse<BirthdayBookingResponse>> reschedule(@PathVariable Integer id,
                                                                             @Valid @RequestBody BirthdayRescheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Birthday booking rescheduled",
                bookingService.reschedule(id, request)));
    }
    @PostMapping("/{id}/invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> issueInvoice(@PathVariable Integer id, @AuthenticationPrincipal String username) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Birthday invoice ready", billingService.issue(id, username)));
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> invoice(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(billingService.getInvoice(id)));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<ApiResponse<InvoiceResponse>> recordPayment(@PathVariable Integer id,
                                                                        @Valid @RequestBody PaymentRequest request,
                                                                        @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                                        @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(ApiResponse.success("Birthday payment recorded",
                billingService.recordPayment(id, request, username, idempotencyKey)));
    }
}
