package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.trial.*;
import com.playville.crm.service.TrialEntitlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/customer-entitlements")
public class CustomerEntitlementController {
    private final TrialEntitlementService service;
    @PreAuthorize("hasAnyRole('admin', 'manager')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<EntitlementResponse>> cancel(@PathVariable Integer id, @Valid @RequestBody ReasonRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Entitlement cancelled", service.cancel(id, request)));
    }
}
