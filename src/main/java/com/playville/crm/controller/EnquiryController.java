package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.common.PagedResponse;
import com.playville.crm.dto.enquiry.EnquiryRequest;
import com.playville.crm.dto.enquiry.EnquiryResponse;
import com.playville.crm.dto.enquiry.EnquiryStatusRequest;
import com.playville.crm.entity.enums.EnquiryStatus;
import com.playville.crm.service.EnquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/enquiries")
@RequiredArgsConstructor
public class EnquiryController {

    private final EnquiryService enquiryService;

    @PostMapping
    public ResponseEntity<ApiResponse<EnquiryResponse>> create(
            @Valid @RequestBody EnquiryRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Enquiry created", enquiryService.create(request, idempotencyKey)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<EnquiryResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) EnquiryStatus status,
            @RequestParam(required = false) String search) {
        Page<EnquiryResponse> result = enquiryService.list(page, size, status, search);
        return ResponseEntity.ok(PagedResponse.of(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EnquiryResponse>> get(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(enquiryService.get(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EnquiryResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody EnquiryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Enquiry updated", enquiryService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<EnquiryResponse>> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody EnquiryStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Enquiry status updated", enquiryService.updateStatus(id, request)));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<ApiResponse<EnquiryResponse>> convert(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Enquiry converted", enquiryService.convert(id)));
    }
}
