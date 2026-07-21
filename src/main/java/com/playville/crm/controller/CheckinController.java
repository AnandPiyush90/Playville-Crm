package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.checkin.*;
import com.playville.crm.service.CheckinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Check-in", description = "Check-in, checkout and session management")
@RestController
@RequestMapping("/checkins")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    @Operation(summary = "Check-in a customer with selected kids")
    @PostMapping
    public ResponseEntity<ApiResponse<CheckinResponse>> checkin(
            @Valid @RequestBody CheckinRequest req,
            @AuthenticationPrincipal String username) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Check-in successful",
                        checkinService.checkin(req, username)));
    }

    @Operation(summary = "Checkout an active check-in")
    @PostMapping("/{checkinId}/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @PathVariable Integer checkinId,
            @Valid @RequestBody CheckoutRequest req,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(ApiResponse.success("Checkout complete",
                checkinService.checkout(checkinId, req, username)));
    }

    @Operation(summary = "Preview an active check-in before checkout")
    @GetMapping("/{checkinId}/checkout-preview")
    public ResponseEntity<ApiResponse<CheckoutPreviewResponse>> checkoutPreview(@PathVariable Integer checkinId) {
        return ResponseEntity.ok(ApiResponse.success(checkinService.checkoutPreview(checkinId)));
    }

    @Operation(summary = "Cancel an active check-in and release a reserved entitlement")
    @PostMapping("/{checkinId}/cancel")
    public ResponseEntity<ApiResponse<CheckinResponse>> cancel(
            @PathVariable Integer checkinId,
            @Valid @RequestBody CancelCheckinRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Check-in cancelled",
                checkinService.cancel(checkinId, request)));
    }

    @Operation(summary = "List all active check-ins for current branch")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CheckinResponse>>> getActive() {
        return ResponseEntity.ok(
                ApiResponse.success(checkinService.getActiveCheckins()));
    }

    @Operation(summary = "Get check-in by ID")
    @GetMapping("/{checkinId}")
    public ResponseEntity<ApiResponse<CheckinResponse>> getById(
            @PathVariable Integer checkinId) {
        return ResponseEntity.ok(
                ApiResponse.success(checkinService.getCheckinById(checkinId)));
    }

    @GetMapping("/{checkinId}/items")
    public ResponseEntity<ApiResponse<List<SessionItemResponse>>> items(@PathVariable Integer checkinId) {
        return ResponseEntity.ok(ApiResponse.success(checkinService.sessionItems(checkinId)));
    }

    @PostMapping("/{checkinId}/items")
    public ResponseEntity<ApiResponse<List<SessionItemResponse>>> addItem(@PathVariable Integer checkinId,
            @Valid @RequestBody SessionItemRequest request, @AuthenticationPrincipal String username) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Item assigned to visit",
                checkinService.addSessionItem(checkinId, request, username)));
    }

    @PutMapping("/{checkinId}/items/{itemId}")
    public ResponseEntity<ApiResponse<List<SessionItemResponse>>> updateItem(@PathVariable Integer checkinId,
            @PathVariable Integer itemId, @Valid @RequestBody SessionItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Visit item updated",
                checkinService.updateSessionItem(checkinId, itemId, request)));
    }

    @DeleteMapping("/{checkinId}/items/{itemId}")
    public ResponseEntity<ApiResponse<List<SessionItemResponse>>> removeItem(@PathVariable Integer checkinId,
            @PathVariable Integer itemId) {
        return ResponseEntity.ok(ApiResponse.success("Visit item removed",
                checkinService.removeSessionItem(checkinId, itemId)));
    }
}
