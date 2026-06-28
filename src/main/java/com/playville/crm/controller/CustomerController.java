package com.playville.crm.controller;

import com.playville.crm.common.*;
import com.playville.crm.dto.customer.*;
import com.playville.crm.dto.kid.*;
import com.playville.crm.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Customers", description = "Customer and kid management — branch-scoped")
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "List customers for current branch (paginated)")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CustomerSummaryDto>>> getAll(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(required = false)    String search) {
        Page<CustomerSummaryDto> result = customerService.getCustomers(page, size, search);
        return ResponseEntity.ok(PagedResponse.of(result));
    }

    @Operation(summary = "Get customer by ID with kids")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getCustomerById(id)));
    }

    @Operation(summary = "Find customer by phone (global — for check-in)")
    @GetMapping("/phone/{phone}")
    public ResponseEntity<ApiResponse<CustomerDto>> getByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(ApiResponse.success(customerService.findByPhone(phone)));
    }

    @Operation(summary = "Register new customer")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDto>> create(
            @Valid @RequestBody CreateCustomerRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer registered",
                        customerService.createCustomer(req)));
    }

    @Operation(summary = "Update customer details")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateCustomerRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success(customerService.updateCustomer(id, req)));
    }

    @Operation(summary = "List kids for a customer")
    @GetMapping("/{customerId}/kids")
    public ResponseEntity<ApiResponse<List<KidDto>>> getKids(
            @PathVariable Integer customerId) {
        return ResponseEntity.ok(
                ApiResponse.success(customerService.getKidsForCustomer(customerId)));
    }

    @Operation(summary = "Add a kid to a customer")
    @PostMapping("/{customerId}/kids")
    public ResponseEntity<ApiResponse<KidDto>> addKid(
            @PathVariable Integer customerId,
            @Valid @RequestBody CreateKidRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Kid added",
                        customerService.addKid(customerId, req)));
    }

    @Operation(summary = "Deactivate a kid (soft delete)")
    @DeleteMapping("/{customerId}/kids/{kidId}")
    public ResponseEntity<ApiResponse<Void>> deactivateKid(
            @PathVariable Integer customerId,
            @PathVariable Integer kidId) {
        customerService.deactivateKid(customerId, kidId);
        return ResponseEntity.ok(ApiResponse.success("Kid deactivated"));
    }
}