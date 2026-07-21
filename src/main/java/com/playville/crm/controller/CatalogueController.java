package com.playville.crm.controller;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.common.PagedResponse;
import com.playville.crm.dto.catalogue.*;
import com.playville.crm.service.CatalogueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class CatalogueController {
    private final CatalogueService catalogueService;

    @GetMapping("/product-categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> categories(@RequestParam(defaultValue = "false") boolean includeInactive) { return ResponseEntity.ok(ApiResponse.success(catalogueService.categories(includeInactive))); }

    @PostMapping("/product-categories") @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Product category created", catalogueService.createCategory(request)));
    }

    @PutMapping("/product-categories/{id}") @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(@PathVariable Integer id, @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product category updated", catalogueService.updateCategory(id, request)));
    }

    @DeleteMapping("/product-categories/{id}") @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<CategoryResponse>> deactivateCategory(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Product category deactivated", catalogueService.setCategoryActive(id, false)));
    }

    @PatchMapping("/product-categories/{id}/reactivate") @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<CategoryResponse>> reactivateCategory(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Product category reactivated", catalogueService.setCategoryActive(id, true)));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> product(@PathVariable Integer id) { return ResponseEntity.ok(ApiResponse.success(catalogueService.getProduct(id))); }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> products(@RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Page<ProductResponse> result = catalogueService.products(search, page, size); return ResponseEntity.ok(PagedResponse.of(result));
    }

    @PostMapping("/products") @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Product created", catalogueService.createProduct(request)));
    }

    @PutMapping("/products/{id}") @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(@PathVariable Integer id, @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product updated", catalogueService.updateProduct(id, request)));
    }

    @DeleteMapping("/products/{id}") @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<ProductResponse>> deactivateProduct(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Product deactivated", catalogueService.setProductActive(id, false)));
    }

    @PatchMapping("/products/{id}/reactivate") @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<ProductResponse>> reactivateProduct(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Product reactivated", catalogueService.setProductActive(id, true)));
    }

    @GetMapping("/tax-profiles")
    public ResponseEntity<ApiResponse<List<TaxProfileResponse>>> taxProfiles() {
        return ResponseEntity.ok(ApiResponse.success(catalogueService.activeTaxProfiles()));
    }

    @PutMapping("/branch-skus/{skuId}") @PreAuthorize("hasAnyRole('admin','manager')")
    public ResponseEntity<ApiResponse<SkuResponse>> configureBranchSku(@PathVariable Integer skuId, @Valid @RequestBody UpdateBranchSkuRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Branch SKU configuration updated", catalogueService.configureCurrentBranchSku(skuId, request)));
    }
}
