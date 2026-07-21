package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.common.PagedResponse;
import com.playville.crm.dto.inventory.*;
import com.playville.crm.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/stock-transfers") @RequiredArgsConstructor
public class StockTransferController {
    private final InventoryService inventoryService;
    @GetMapping @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<PagedResponse<StockTransferResponse>>> list(@RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return ResponseEntity.ok(PagedResponse.of(inventoryService.transfers(status, page, size))); }
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> get(@PathVariable Integer id) { return ResponseEntity.ok(ApiResponse.success(inventoryService.transfer(id))); }
    @PostMapping @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> create(@Valid @RequestBody CreateStockTransferRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey, @AuthenticationPrincipal String username) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Stock transfer created", inventoryService.createTransfer(request, username, idempotencyKey))); }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> update(@PathVariable Integer id, @Valid @RequestBody CreateStockTransferRequest request) { return ResponseEntity.ok(ApiResponse.success("Stock transfer updated", inventoryService.updateTransfer(id, request))); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) { inventoryService.deleteTransfer(id); return ResponseEntity.ok(ApiResponse.success("Stock transfer deleted", null)); }
    @PostMapping("/{id}/dispatch") @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> dispatch(@PathVariable Integer id, @RequestHeader("Idempotency-Key") String idempotencyKey, @AuthenticationPrincipal String username) { return ResponseEntity.ok(ApiResponse.success("Stock transfer dispatched", inventoryService.dispatchTransfer(id, username, idempotencyKey))); }
    @PostMapping("/{id}/receive") @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> receive(@PathVariable Integer id, @Valid @RequestBody ReceiveStockTransferRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey, @AuthenticationPrincipal String username) { return ResponseEntity.ok(ApiResponse.success("Stock transfer received", inventoryService.receiveTransfer(id, request, username, idempotencyKey))); }
}
