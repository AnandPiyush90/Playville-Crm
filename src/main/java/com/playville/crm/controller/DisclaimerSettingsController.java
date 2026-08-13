package com.playville.crm.controller;
import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.disclaimer.DisclaimerSettingsRequest;
import com.playville.crm.dto.disclaimer.DisclaimerSettingsResponse;
import com.playville.crm.service.DisclaimerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/branches/current/disclaimer-settings") public class DisclaimerSettingsController {
    private final DisclaimerService service;
    @GetMapping public ApiResponse<DisclaimerSettingsResponse> get(){
        return ApiResponse.success(service.getSettings());
    }
    @PutMapping @PreAuthorize("hasRole('admin')") public ApiResponse<DisclaimerSettingsResponse> update(@Valid @RequestBody DisclaimerSettingsRequest request){
        return ApiResponse.success("Disclaimer settings updated", service.settings(request));
    }
}
