package com.playville.crm.controller;

import com.playville.crm.common.*;
import com.playville.crm.dto.report.*;
import com.playville.crm.service.TrialReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController @RequiredArgsConstructor @RequestMapping("/reports")
public class TrialReportController {
    private final TrialReportingService reports;
    @GetMapping("/trial-funnel")
    public ResponseEntity<ApiResponse<TrialFunnelResponse>> funnel(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) Integer staffId,
            @RequestParam(required = false) String leadSource,
            @RequestParam(required = false) String campaignCode) {
        return ResponseEntity.ok(ApiResponse.success(reports.funnel(from, to, branchId, staffId, leadSource, campaignCode)));
    }

    @GetMapping("/trial-conversions")
    public ResponseEntity<ApiResponse<PagedResponse<TrialConversionResponse>>> conversions(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) Integer staffId,
            @RequestParam(required = false) String leadSource,
            @RequestParam(required = false) String campaignCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TrialConversionResponse> result = reports.conversions(from, to, branchId, staffId, leadSource, campaignCode, page, size);
        return ResponseEntity.ok(PagedResponse.of(result));
    }
}
