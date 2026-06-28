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
}