package com.playville.crm.controller;

import com.playville.crm.common.*;
import com.playville.crm.dto.purchase.*;
import com.playville.crm.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Purchases", description = "Package purchases and recharges")
@RestController
@RequestMapping("/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @Operation(summary = "Purchase a package for a customer")
    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseResponse>> purchase(
            @Valid @RequestBody PurchaseRequest req,
            @AuthenticationPrincipal String username) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Package purchased successfully",
                        purchaseService.purchasePackage(req, username)));
    }

    @Operation(summary = "List purchases for current branch (paginated)")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PurchaseResponse>>> getBranchPurchases(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PurchaseResponse> result = purchaseService.getBranchPurchases(page, size);
        return ResponseEntity.ok(PagedResponse.of(result));
    }

    @Operation(summary = "Get purchase history for a customer")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getCustomerHistory(
            @PathVariable Integer customerId) {
        return ResponseEntity.ok(ApiResponse.success(
                purchaseService.getCustomerPurchaseHistory(customerId)));
    }
}