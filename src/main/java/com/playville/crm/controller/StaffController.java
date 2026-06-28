package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.dto.staff.*;
import com.playville.crm.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Staff", description = "Staff management — manager and admin")
@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @Operation(summary = "List staff for current branch")
    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(staffService.getStaffForCurrentBranch()));
    }

    @Operation(summary = "Create staff for current branch")
    @PostMapping
    @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<StaffDto>> create(
            @Valid @RequestBody CreateStaffRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff created", staffService.createStaff(req)));
    }

    @Operation(summary = "Deactivate staff")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Integer id) {
        staffService.deactivateStaff(id);
        return ResponseEntity.ok(ApiResponse.success("Staff deactivated"));
    }
}