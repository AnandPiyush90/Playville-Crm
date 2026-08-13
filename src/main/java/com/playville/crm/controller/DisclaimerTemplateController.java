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
@RestController @RequiredArgsConstructor @RequestMapping("/disclaimer-templates") public class DisclaimerTemplateController {
    private final DisclaimerService service;
    @GetMapping @PreAuthorize("hasAnyRole('admin','manager')") public ApiResponse<List<DisclaimerView>> list(){
        return ApiResponse.success(service.listTemplates());
    }
    @PostMapping @PreAuthorize("hasRole('admin')") public ResponseEntity<ApiResponse<DisclaimerView>> create(@Valid @RequestBody TemplateRequest r){
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.createTemplate(r)));
    }
    @PutMapping("/{id}") @PreAuthorize("hasRole('admin')") public ApiResponse<DisclaimerView> update(@PathVariable Long id,@Valid @RequestBody TemplateRequest r){
        return ApiResponse.success(service.updateTemplate(id,r));
    }
    @PostMapping("/{id}/publish") @PreAuthorize("hasRole('admin')") public ApiResponse<DisclaimerView> publish(@PathVariable Long id){
        return ApiResponse.success(service.publish(id));
    }
}
