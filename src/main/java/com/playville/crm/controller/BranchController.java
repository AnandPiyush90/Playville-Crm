package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.branch.*;
import com.playville.crm.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Branches", description = "Branch management — admin only")
@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @Operation(summary = "List all active branches")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(branchService.getAllActiveBranches()));
    }

    @Operation(summary = "Get branch by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchDto>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(branchService.getBranchById(id)));
    }

    @Operation(summary = "Update branch details")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<BranchDto>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateBranchRequest req) {
        return ResponseEntity.ok(ApiResponse.success(branchService.updateBranch(id, req)));
    }
}
