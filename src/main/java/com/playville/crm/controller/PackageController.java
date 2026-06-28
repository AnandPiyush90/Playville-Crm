package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.packages.*;
import com.playville.crm.service.PackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Packages", description = "Session package management — admin only for CUD")
@RestController
@RequestMapping("/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @Operation(summary = "List all active packages — public for staff")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PackageResponse>>> getActive() {
        return ResponseEntity.ok(
                ApiResponse.success(packageService.getAllActivePackages()));
    }

    @Operation(summary = "List all packages including inactive — admin only")
    @GetMapping("/all")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<List<PackageResponse>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success(packageService.getAllPackages()));
    }

    @Operation(summary = "Get package by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PackageResponse>> getById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(packageService.getById(id)));
    }

    @Operation(summary = "Create a new package — admin only")
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<PackageResponse>> create(
            @Valid @RequestBody PackageRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Package created",
                        packageService.createPackage(req)));
    }

    @Operation(summary = "Update package details — admin only")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<PackageResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody PackageRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Package updated",
                packageService.updatePackage(id, req)));
    }

    @Operation(summary = "Deactivate a package — admin only")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable Integer id) {
        packageService.deactivatePackage(id);
        return ResponseEntity.ok(
                ApiResponse.success("Package deactivated"));
    }

    @Operation(summary = "Reactivate a package — admin only")
    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<PackageResponse>> reactivate(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Package reactivated",
                packageService.reactivatePackage(id)));
    }
}