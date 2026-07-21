package com.playville.crm.controller;
import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.birthday.*;
import com.playville.crm.service.BirthdayDecorationPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/birthday-decoration-packages") @RequiredArgsConstructor
public class BirthdayDecorationPackageController {
    private final BirthdayDecorationPackageService service;
    @GetMapping public ResponseEntity<ApiResponse<List<BirthdayDecorationPackageResponse>>> active() { return ResponseEntity.ok(ApiResponse.success(service.active())); }
    @GetMapping("/all") @PreAuthorize("hasAnyRole('admin','manager')") public ResponseEntity<ApiResponse<List<BirthdayDecorationPackageResponse>>> all() { return ResponseEntity.ok(ApiResponse.success(service.all())); }
    @PostMapping @PreAuthorize("hasAnyRole('admin','manager')") public ResponseEntity<ApiResponse<BirthdayDecorationPackageResponse>> create(@Valid @RequestBody BirthdayDecorationPackageRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Decoration package created", service.create(request))); }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('admin','manager')") public ResponseEntity<ApiResponse<BirthdayDecorationPackageResponse>> update(@PathVariable Integer id, @Valid @RequestBody BirthdayDecorationPackageRequest request) { return ResponseEntity.ok(ApiResponse.success("Decoration package updated", service.update(id, request))); }
    @PostMapping("/{id}/deactivate") @PreAuthorize("hasAnyRole('admin','manager')") public ResponseEntity<ApiResponse<BirthdayDecorationPackageResponse>> deactivate(@PathVariable Integer id) { return ResponseEntity.ok(ApiResponse.success("Decoration package deactivated", service.deactivate(id))); }
}
