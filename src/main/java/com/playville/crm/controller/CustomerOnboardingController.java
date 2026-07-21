package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.onboarding.*;
import com.playville.crm.service.CustomerOnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @RequestMapping("/customer-onboarding")
public class CustomerOnboardingController {
    private final CustomerOnboardingService service;
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerOnboardingResponse>> onboard(
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody CustomerOnboardingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Customer onboarding complete", service.onboard(request, key)));
    }
}
