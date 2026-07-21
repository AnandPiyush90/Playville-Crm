package com.playville.crm.controller;

import com.playville.crm.common.*;
import com.playville.crm.dto.inventory.*;
import com.playville.crm.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.time.LocalDate;
import com.playville.crm.entity.enums.InventoryMovementType;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;
    @GetMapping({"", "/stock"}) public ResponseEntity<ApiResponse<PagedResponse<InventoryBalanceResponse>>> balances(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false) LocalDate expiringBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PagedResponse.of(inventoryService.balances(search, categoryId, lowStock, expiringBefore, page, size)));
    }
    @GetMapping("/alerts") public ResponseEntity<ApiResponse<List<InventoryBalanceResponse>>> alerts() { return ResponseEntity.ok(ApiResponse.success(inventoryService.alerts())); }
    @GetMapping("/suppliers") @PreAuthorize("hasAnyRole('admin','manager')") public ResponseEntity<ApiResponse<List<SupplierResponse>>> suppliers(@RequestParam(defaultValue = "false") boolean includeInactive) { return ResponseEntity.ok(ApiResponse.success(inventoryService.suppliers(includeInactive))); }
    @GetMapping("/suppliers/{id}") @PreAuthorize("hasAnyRole('admin','manager')") public ResponseEntity<ApiResponse<SupplierResponse>> supplier(@PathVariable Integer id) { return ResponseEntity.ok(ApiResponse.success(inventoryService.supplier(id))); }
    @PostMapping("/suppliers") @PreAuthorize("hasAnyRole('admin','manager')") public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(@Valid @RequestBody CreateSupplierRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Supplier created", inventoryService.createSupplier(request))); }
    @PutMapping("/suppliers/{id}") @PreAuthorize("hasAnyRole('admin','manager')") public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(@PathVariable Integer id, @Valid @RequestBody CreateSupplierRequest request) { return ResponseEntity.ok(ApiResponse.success("Supplier updated", inventoryService.updateSupplier(id, request))); }
    @DeleteMapping("/suppliers/{id}") @PreAuthorize("hasAnyRole('admin','manager')") public ResponseEntity<ApiResponse<Void>> deactivateSupplier(@PathVariable Integer id) { inventoryService.deactivateSupplier(id); return ResponseEntity.ok(ApiResponse.success("Supplier deactivated", null)); }
    @PatchMapping("/suppliers/{id}/reactivate") @PreAuthorize("hasAnyRole('admin','manager')") public ResponseEntity<ApiResponse<SupplierResponse>> reactivateSupplier(@PathVariable Integer id) { return ResponseEntity.ok(ApiResponse.success("Supplier reactivated", inventoryService.reactivateSupplier(id))); }
    @PostMapping("/adjustments") @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<InventoryBalanceResponse>> adjust(@Valid @RequestBody StockAdjustmentRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey, @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(ApiResponse.success("Stock updated", inventoryService.adjust(request, username, idempotencyKey)));
    }
    @PostMapping("/receipts") @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<StockReceiptResponse>> receive(@Valid @RequestBody CreateStockReceiptRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey, @AuthenticationPrincipal String username) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Stock received", inventoryService.receive(request, username, idempotencyKey)));
    }
    @GetMapping("/receipts") @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<PagedResponse<StockReceiptResponse>>> receipts(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return ResponseEntity.ok(PagedResponse.of(inventoryService.receipts(page, size))); }
    @GetMapping("/receipts/{id}") @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<StockReceiptResponse>> receipt(@PathVariable Integer id) { return ResponseEntity.ok(ApiResponse.success(inventoryService.receipt(id))); }
    @GetMapping("/skus/{skuId}/movements") @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<PagedResponse<InventoryMovementResponse>>> movements(@PathVariable Integer skuId,
            @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) InventoryMovementType type,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Page<InventoryMovementResponse> result = inventoryService.movements(skuId, from, to, type, page, size); return ResponseEntity.ok(PagedResponse.of(result));
    }
}
