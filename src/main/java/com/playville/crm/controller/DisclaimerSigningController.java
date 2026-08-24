package com.playville.crm.controller;
import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.disclaimer.*;
import com.playville.crm.service.DisclaimerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequiredArgsConstructor public class DisclaimerSigningController {
    private final DisclaimerService service;
    @PostMapping("/customer-onboarding/drafts") public ResponseEntity<ApiResponse<DisclaimerView>> draft(@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody DraftRequest r){
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.createDraft(r,key)));
    }
    @GetMapping("/customer-onboarding/drafts/{id}") public ApiResponse<DisclaimerView> draft(@PathVariable Long id){
        return ApiResponse.success(service.getDraft(id));
    }
    @PostMapping("/customer-onboarding/drafts/{id}/tablet-request") public ApiResponse<DisclaimerView> request(@PathVariable Long id,@RequestHeader(value="Idempotency-Key",required=false) String key){
        return ApiResponse.success(service.tabletRequest(id,key));
    }
    @PostMapping("/customer-onboarding/drafts/{id}/email-request") public ApiResponse<DisclaimerView> emailRequest(@PathVariable Long id,@RequestHeader(value="Idempotency-Key",required=false) String key){
        return ApiResponse.success("Disclaimer email queued",service.emailRequest(id,key));
    }
    @GetMapping("/disclaimer-signing-requests/{id}/tablet-view") public ApiResponse<DisclaimerView> view(@PathVariable Long id){
        return ApiResponse.success(service.tabletView(id));
    }
    @PostMapping("/disclaimer-signing-requests/{id}/tablet-acceptance") public ApiResponse<DisclaimerView> accept(@PathVariable Long id,@Valid @RequestBody TabletAcceptanceRequest r){
        return ApiResponse.success(service.accept(id,r));
    }
    @GetMapping("/customers/{id}/disclaimer-acceptances") public ApiResponse<List<DisclaimerView>> history(@PathVariable Integer id){
        return ApiResponse.success(service.customerAcceptances(id));
    }
    @GetMapping("/disclaimer-acceptances") @PreAuthorize("hasAnyRole('admin','manager')") public ApiResponse<List<DisclaimerView>> branchHistory(){
        return ApiResponse.success(service.branchAcceptances());
    }
}
