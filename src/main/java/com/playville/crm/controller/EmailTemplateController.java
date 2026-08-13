package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.notification.*;
import com.playville.crm.service.EmailTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/system-settings/email-templates") @PreAuthorize("hasRole('admin')")
public class EmailTemplateController {
    private final EmailTemplateService service;
    @GetMapping public ApiResponse<List<EmailTemplateResponse>> list(){return ApiResponse.success(service.listCurrent());}
    @GetMapping("/{key}") public ApiResponse<EmailTemplateResponse> get(@PathVariable String key){return ApiResponse.success(service.getCurrent(key));}
    @PutMapping("/{key}") public ApiResponse<EmailTemplateResponse> save(@PathVariable String key,@Valid @RequestBody EmailTemplateRequest request){return ApiResponse.success("Email template saved",service.saveCurrent(key,request));}
    @PostMapping("/{key}/preview") public ApiResponse<RenderedEmailTemplate> preview(@PathVariable String key,@RequestBody(required=false) EmailTemplatePreviewRequest request){return ApiResponse.success(service.previewCurrent(key,request));}
    @DeleteMapping("/{key}") public ApiResponse<EmailTemplateResponse> reset(@PathVariable String key){return ApiResponse.success("Email template reset to default",service.resetCurrent(key));}
}
