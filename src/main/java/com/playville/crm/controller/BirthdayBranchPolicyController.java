package com.playville.crm.controller;
import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.birthday.*;
import com.playville.crm.service.BirthdayBranchPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/birthday-booking-policy") @RequiredArgsConstructor
public class BirthdayBranchPolicyController {
    private final BirthdayBranchPolicyService service;
    @GetMapping @PreAuthorize("hasAnyRole('admin','manager')") public ResponseEntity<ApiResponse<BirthdayBranchPolicyResponse>> get() { return ResponseEntity.ok(ApiResponse.success(service.get())); }
    @PutMapping @PreAuthorize("hasAnyRole('admin','manager')") public ResponseEntity<ApiResponse<BirthdayBranchPolicyResponse>> update(@Valid @RequestBody BirthdayBranchPolicyRequest request) { return ResponseEntity.ok(ApiResponse.success("Birthday policy updated", service.update(request))); }
}
