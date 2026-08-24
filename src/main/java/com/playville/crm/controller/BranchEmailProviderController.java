package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.notification.*;
import com.playville.crm.service.BranchEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @RequestMapping("/branches/current/email-provider") @PreAuthorize("hasRole('admin')")
public class BranchEmailProviderController {
    private final BranchEmailService service;
    @GetMapping public ApiResponse<EmailProviderConfigResponse> get(){ return ApiResponse.success(service.getCurrent()); }
    @PutMapping public ApiResponse<EmailProviderConfigResponse> save(@Valid @RequestBody EmailProviderConfigRequest request){ return ApiResponse.success("Email provider saved",service.saveCurrent(request)); }
    @PostMapping("/test") public ApiResponse<EmailProviderConfigResponse> test(@Valid @RequestBody EmailTestRequest request){
        EmailProviderConfigResponse result=service.testCurrent(request);
        String message="SUCCESS".equals(result.getLastTestStatus())?"Test email sent":(result.getLastErrorMessage()!=null?result.getLastErrorMessage():"Test email failed");
        return ApiResponse.success(message, result);
    }
}
