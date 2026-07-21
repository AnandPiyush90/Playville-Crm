package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.trial.*;
import com.playville.crm.entity.enums.EntitlementStatus;
import com.playville.crm.entity.enums.EntitlementType;
import com.playville.crm.service.TrialEntitlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/customers/{customerId}")
public class TrialEntitlementController {
    private final TrialEntitlementService service;
    @GetMapping("/trial-eligibility") public ResponseEntity<ApiResponse<TrialEligibilityResponse>> eligibility(@PathVariable Integer customerId, @RequestParam List<Integer> kidIds) { return ResponseEntity.ok(ApiResponse.success(service.eligibility(customerId, kidIds))); }
    @PostMapping("/trial-entitlements") public ResponseEntity<ApiResponse<EntitlementResponse>> issue(@PathVariable Integer customerId, @Valid @RequestBody IssueTrialRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Trial entitlement issued", service.issue(customerId, request))); }
    @GetMapping("/entitlements")
    public ResponseEntity<ApiResponse<List<EntitlementResponse>>> entitlements(
            @PathVariable Integer customerId,
            @RequestParam(required = false) EntitlementStatus status,
            @RequestParam(required = false) EntitlementType type) {
        return ResponseEntity.ok(ApiResponse.success(service.entitlements(customerId, status, type)));
    }
    @PreAuthorize("hasAnyRole('admin', 'manager')")
    @PostMapping("/complimentary-entitlements") public ResponseEntity<ApiResponse<EntitlementResponse>> manual(@PathVariable Integer customerId, @Valid @RequestBody ManualComplimentaryRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Manual complimentary entitlement issued", service.grantManualComplimentary(customerId, request))); }
}
