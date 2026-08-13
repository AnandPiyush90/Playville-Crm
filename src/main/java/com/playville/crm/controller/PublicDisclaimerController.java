package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.disclaimer.DisclaimerView;
import com.playville.crm.dto.disclaimer.EmailAcceptanceRequest;
import com.playville.crm.service.DisclaimerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public/disclaimer")
public class PublicDisclaimerController {
    private final DisclaimerService service;

    @GetMapping("/{token}")
    public ApiResponse<DisclaimerView> view(@PathVariable String token) {
        return ApiResponse.success(service.publicView(token));
    }

    @PostMapping("/{token}/accept")
    public ApiResponse<DisclaimerView> accept(@PathVariable String token,
                                               @Valid @RequestBody EmailAcceptanceRequest request) {
        return ApiResponse.success("Disclaimer accepted", service.emailAccept(token, request));
    }
}
