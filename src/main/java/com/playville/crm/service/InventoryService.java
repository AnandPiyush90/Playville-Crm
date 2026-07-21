package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.inventory.*;
import com.playville.crm.entity.*;
import com.playville.crm.entity.enums.InventoryMovementType;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductSkuRepository skuRepository;
    private final BranchRepository branchRepository;
    private final BranchSkuRepository branchSkuRepository;
    private final StaffRepository staffRepository;
    private final SupplierRepository supplierRepository;
    private final StockReceiptRepository receiptRepository;
    private final InventoryBatchRepository batchRepository;
    private final StockTransferRepository transferRepository;

    @Transactional(readOnly = true)
    public List<InventoryBalanceResponse> balances() {
        Integer branchId = BranchContext.getBranchId();
        return balanceRepository.findByBranchIdOrderBySkuProductProductNameAsc(branchId).stream().map(this::toBalanceResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<InventoryBalanceResponse> balances(String search, Integer categoryId, Boolean lowStock,
                                                    LocalDate expiringBefore, int page, int size) {
        List<InventoryBalanceResponse> filtered = balances().stream()
                .filter(balance -> search == null || search.isBlank()
                        || balance.getSkuCode().toLowerCase().contains(search.trim().toLowerCase())
                        || balance.getProductName().toLowerCase().contains(search.trim().toLowerCase()))
                .filter(balance -> categoryId == null || skuRepository.findById(balance.getSkuId())
                        .map(sku -> sku.getProduct().getCategory().getId().equals(categoryId)).orElse(false))
                .filter(balance -> lowStock == null || balance.isLowStock() == lowStock)
                .filter(balance -> expiringBefore == null || (balance.getNearestExpiry() != null
                        && !balance.getNearestExpiry().isAfter(expiringBefore)))
                .toList();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        int fromIndex = (int) Math.min(pageable.getOffset(), filtered.size());
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(fromIndex, toIndex), pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public List<InventoryBalanceResponse> alerts() {
        LocalDate threshold = LocalDate.now().plusDays(30);
        return balances().stream()
                .filter(balance -> balance.isLowStock() || balance.isOutOfStock()
                        || (balance.getNearestExpiry() != null && !balance.getNearestExpiry().isAfter(threshold)))
                .toList();
    }

    @Transactional
    public InventoryBalanceResponse adjust(StockAdjustmentRequest request, String username, String idempotencyKey) {
        String key = blankToNull(idempotencyKey);
        if (key == null) throw new BusinessRuleException("IDEMPOTENCY_KEY_REQUIRED: Idempotency-Key header is required to adjust stock");
        var existing = movementRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            InventoryMovement previous = existing.get();
            if (!previous.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
            if (!previous.getSku().getId().equals(request.getSkuId())) {
                throw new ApiConflictException("IDEMPOTENCY_KEY_REUSED", "Idempotency-Key was already used for a different SKU adjustment");
            }
            InventoryBalance existingBalance = balanceRepository.findByBranchIdAndSkuId(previous.getBranch().getId(), previous.getSku().getId())
                        .orElseThrow(() -> new IllegalStateException("Inventory movement exists without a balance"));
            return toBalanceResponse(existingBalance);
        }
        if (!isManualMovement(request.getMovementType()))
            throw new BusinessRuleException("INVALID_STOCK_MOVEMENT: This endpoint only accepts opening stock, adjustments, wastage, or expiry");
        Integer branchId = BranchContext.getBranchId();
        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        ProductSku sku = skuRepository.findById(request.getSkuId()).orElseThrow(() -> new ResourceNotFoundException("SKU", request.getSkuId()));
        if (!sku.getProduct().isTrackInventory()) throw new BusinessRuleException("INVENTORY_NOT_TRACKED: This product does not track inventory");
        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username).orElse(null);
        BranchSku branchSku = branchSkuRepository.findByBranchIdAndSkuId(branchId, sku.getId()).orElseGet(() -> branchSkuRepository.save(BranchSku.builder().branch(branch).sku(sku).build()));
        InventoryBalance balance = balanceRepository.findByBranchIdAndSkuIdForUpdate(branchId, sku.getId()).orElseGet(() ->
                balanceRepository.saveAndFlush(InventoryBalance.builder().branch(branch).sku(sku).build()));
        BigDecimal delta = isDecrease(request.getMovementType()) ? request.getQuantity().negate() : request.getQuantity();
        InventoryBatch batch = request.getBatchId() == null ? null : batchRepository.findByIdForUpdate(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory batch", request.getBatchId()));
        if (batch != null && (!batch.getBranch().getId().equals(branchId) || !batch.getSku().getId().equals(sku.getId()))) {
            throw new BusinessRuleException("ADJUSTMENT_BATCH_MISMATCH: Batch must belong to the current branch and SKU");
        }
        if (isDecrease(request.getMovementType()) && sku.isHasExpiry() && batch == null) {
            throw new BusinessRuleException("BATCH_REQUIRED: A batch is required when reducing expiry-controlled inventory");
        }
        if (isDecrease(request.getMovementType()) && batch != null) {
            if (batch.getQuantityRemaining().compareTo(request.getQuantity()) < 0) {
                throw new ApiConflictException("INSUFFICIENT_STOCK", "Insufficient stock in the selected batch");
            }
            batch.setQuantityRemaining(batch.getQuantityRemaining().subtract(request.getQuantity()));
            batchRepository.save(batch);
        }
        if (!branchSku.isAllowNegativeStock() && balance.availableQuantity().add(delta).signum() < 0)
            throw new ApiConflictException("INSUFFICIENT_STOCK", "Adjustment would reduce stock below zero");
        balance.setQuantityOnHand(balance.getQuantityOnHand().add(delta));
        balanceRepository.save(balance);
        movementRepository.save(InventoryMovement.builder().branch(branch).sku(sku).batch(batch).movementType(request.getMovementType())
                .quantityDelta(delta).unitCostSnapshot(sku.getDefaultCostPrice()).referenceType("MANUAL_STOCK_OPERATION")
                .reasonCode(request.getReasonCode().trim()).notes(request.getNotes().trim()).performedByStaff(staff)
                .idempotencyKey(key).build());
        return toBalanceResponse(balance);
    }

    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> movements(Integer skuId, LocalDate from, LocalDate to,
                                                      InventoryMovementType type, int page, int size) {
        skuRepository.findById(skuId).orElseThrow(() -> new ResourceNotFoundException("SKU", skuId));
        if (from != null && to != null && to.isBefore(from)) {
            throw new BusinessRuleException("INVALID_DATE_RANGE: To date must be on or after from date");
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return movementRepository.search(BranchContext.getBranchId(), skuId,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(), type, pageable).map(this::toMovementResponse);
    }

    @Transactional
    public StockReceiptResponse receive(CreateStockReceiptRequest request, String username, String idempotencyKey) {
        String key = blankToNull(idempotencyKey);
        if (key == null) throw new BusinessRuleException("IDEMPOTENCY_KEY_REQUIRED: Idempotency-Key header is required to receive stock");
        var existing = receiptRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            if (!existing.get().getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
            return receiptResponse(existing.get());
        }
        Integer branchId = BranchContext.getBranchId();
        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        Supplier supplier = request.getSupplierId() == null ? null : supplierRepository.findById(request.getSupplierId()).orElseThrow(() -> new ResourceNotFoundException("Supplier", request.getSupplierId()));
        if (supplier != null && !supplier.isActive()) throw new BusinessRuleException("SUPPLIER_INACTIVE: Supplier is not active");
        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username).orElse(null);
        StockReceipt receipt = receiptRepository.saveAndFlush(StockReceipt.builder().branch(branch).supplier(supplier)
                .supplierInvoiceReference(blankToNull(request.getSupplierInvoiceReference())).receivedAt(request.getReceivedAt() == null ? java.time.LocalDateTime.now() : request.getReceivedAt())
                .notes(blankToNull(request.getNotes())).receivedByStaff(staff).idempotencyKey(key).build());
        List<Integer> batchIds = new java.util.ArrayList<>();
        for (StockReceiptLineRequest line : request.getItems()) {
            ProductSku sku = skuRepository.findById(line.getSkuId()).orElseThrow(() -> new ResourceNotFoundException("SKU", line.getSkuId()));
            if (!sku.getProduct().isTrackInventory()) throw new BusinessRuleException("INVENTORY_NOT_TRACKED: SKU does not track inventory");
            if (sku.isHasExpiry() && (line.getBatchNumber() == null || line.getBatchNumber().isBlank() || line.getExpiresOn() == null))
                throw new BusinessRuleException("BATCH_DETAILS_REQUIRED: Batch number and expiry date are required for this SKU");
            if (line.getExpiresOn() != null && !line.getExpiresOn().isAfter(java.time.LocalDate.now())) throw new BusinessRuleException("BATCH_EXPIRED: Expiry date must be in the future");
            InventoryBalance balance = balanceRepository.findByBranchIdAndSkuIdForUpdate(branchId, sku.getId()).orElseGet(() -> balanceRepository.saveAndFlush(InventoryBalance.builder().branch(branch).sku(sku).build()));
            InventoryBatch batch = batchRepository.save(InventoryBatch.builder().branch(branch).sku(sku).supplier(supplier).stockReceipt(receipt)
                    .batchNumber(blankToNull(line.getBatchNumber())).receivedAt(receipt.getReceivedAt()).manufacturedOn(line.getManufacturedOn()).expiresOn(line.getExpiresOn())
                    .unitCost(line.getUnitCost() == null ? sku.getDefaultCostPrice() : line.getUnitCost()).quantityReceived(line.getQuantity()).quantityRemaining(line.getQuantity()).build());
            balance.setQuantityOnHand(balance.getQuantityOnHand().add(line.getQuantity()));
            balanceRepository.save(balance);
            movementRepository.save(InventoryMovement.builder().branch(branch).sku(sku).batch(batch).movementType(InventoryMovementType.RECEIPT).quantityDelta(line.getQuantity())
                    .unitCostSnapshot(batch.getUnitCost()).referenceType("STOCK_RECEIPT").referenceId(receipt.getId()).performedByStaff(staff).build());
            batchIds.add(batch.getId());
        }
        return receiptResponse(receipt, batchIds);
    }

    @Transactional(readOnly = true)
    public Page<StockReceiptResponse> receipts(int page, int size) {
        Pageable pageable = safePage(page, size);
        return receiptRepository.findByBranchIdOrderByReceivedAtDesc(BranchContext.getBranchId(), pageable)
                .map(this::receiptResponse);
    }

    @Transactional(readOnly = true)
    public StockReceiptResponse receipt(Integer id) {
        StockReceipt receipt = receiptRepository.findByIdAndBranchId(id, BranchContext.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock receipt", id));
        return receiptResponse(receipt);
    }

    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest request) {
        String name = request.getSupplierName().trim();
        if (supplierRepository.existsBySupplierNameIgnoreCase(name)) throw new ApiConflictException("SUPPLIER_EXISTS", "A supplier with this name already exists");
        Supplier supplier = supplierRepository.save(Supplier.builder().supplierName(name).phone(blankToNull(request.getPhone())).email(blankToNull(request.getEmail())).taxNumber(blankToNull(request.getTaxNumber())).build());
        return supplierResponse(supplier);
    }

    @Transactional(readOnly = true)
    public SupplierResponse supplier(Integer id) {
        return supplierResponse(supplierRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Supplier", id)));
    }

    @Transactional
    public SupplierResponse updateSupplier(Integer id, CreateSupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
        String name = request.getSupplierName().trim();
        if (supplierRepository.existsBySupplierNameIgnoreCaseAndIdNot(name, id))
            throw new ApiConflictException("SUPPLIER_EXISTS", "A supplier with this name already exists");
        supplier.setSupplierName(name); supplier.setPhone(blankToNull(request.getPhone()));
        supplier.setEmail(blankToNull(request.getEmail())); supplier.setTaxNumber(blankToNull(request.getTaxNumber()));
        return supplierResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public void deactivateSupplier(Integer id) {
        Supplier supplier = supplierRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
        supplier.setActive(false); supplierRepository.save(supplier);
    }

    @Transactional
    public SupplierResponse reactivateSupplier(Integer id) {
        Supplier supplier = supplierRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
        supplier.setActive(true); return supplierResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public StockTransferResponse createTransfer(CreateStockTransferRequest request, String username, String idempotencyKey) {
        String key = blankToNull(idempotencyKey); if (key == null) throw new BusinessRuleException("IDEMPOTENCY_KEY_REQUIRED: Idempotency-Key header is required to create a transfer");
        Branch source = branchRepository.findById(BranchContext.getBranchId()).orElseThrow(() -> new ResourceNotFoundException("Branch", BranchContext.getBranchId()));
        var existing = transferRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            if (!existing.get().getSourceBranch().getId().equals(source.getId())) throw new BranchAccessDeniedException();
            return transferResponse(existing.get());
        }
        Branch destination = branchRepository.findById(request.getDestinationBranchId()).orElseThrow(() -> new ResourceNotFoundException("Branch", request.getDestinationBranchId()));
        if (source.getId().equals(destination.getId())) throw new BusinessRuleException("INVALID_TRANSFER_BRANCH: Source and destination branches must differ");
        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username).orElse(null);
        String transferNumber = "TRF-" + source.getBranchCode() + "-" + DateTimeFormatter.BASIC_ISO_DATE.format(java.time.LocalDate.now())
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        StockTransfer transfer = StockTransfer.builder().transferNumber(transferNumber).sourceBranch(source).destinationBranch(destination).notes(blankToNull(request.getNotes())).createdByStaff(staff).idempotencyKey(key).build();
        transferRepository.saveAndFlush(transfer);
        for (StockTransferLineRequest line : request.getItems()) {
            ProductSku sku = skuRepository.findById(line.getSkuId()).orElseThrow(() -> new ResourceNotFoundException("SKU", line.getSkuId()));
            InventoryBatch batch = line.getBatchId() == null ? null : batchRepository.findById(line.getBatchId()).orElseThrow(() -> new ResourceNotFoundException("Inventory batch", line.getBatchId()));
            if (batch != null && (!batch.getBranch().getId().equals(source.getId()) || !batch.getSku().getId().equals(sku.getId()))) throw new BusinessRuleException("TRANSFER_BATCH_MISMATCH: Batch must belong to the source branch and SKU");
            if (sku.isHasExpiry() && batch == null) throw new BusinessRuleException("TRANSFER_BATCH_REQUIRED: A batch is required for expiry-controlled SKUs");
            transfer.getItems().add(StockTransferItem.builder().transfer(transfer).sku(sku).batch(batch).requestedQuantity(line.getQuantity()).build());
        }
        return transferResponse(transferRepository.save(transfer));
    }

    @Transactional(readOnly = true)
    public Page<StockTransferResponse> transfers(String status, int page, int size) {
        String normalizedStatus = blankToNull(status);
        if (normalizedStatus != null) normalizedStatus = normalizedStatus.toUpperCase();
        return transferRepository.findAccessible(BranchContext.getBranchId(), normalizedStatus, safePage(page, size)).map(this::transferResponse);
    }

    @Transactional(readOnly = true)
    public StockTransferResponse transfer(Integer id) {
        return transferResponse(transferRepository.findAccessibleById(id, BranchContext.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock transfer", id)));
    }

    @Transactional
    public StockTransferResponse updateTransfer(Integer id, CreateStockTransferRequest request) {
        StockTransfer transfer = transferRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Stock transfer", id));
        if (!transfer.getSourceBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
        if (!"DRAFT".equals(transfer.getStatus())) throw new ApiConflictException("TRANSFER_NOT_EDITABLE", "Only a draft transfer can be updated");
        Branch destination = branchRepository.findById(request.getDestinationBranchId()).orElseThrow(() -> new ResourceNotFoundException("Branch", request.getDestinationBranchId()));
        if (transfer.getSourceBranch().getId().equals(destination.getId())) throw new BusinessRuleException("INVALID_TRANSFER_BRANCH: Source and destination branches must differ");
        transfer.setDestinationBranch(destination); transfer.setNotes(blankToNull(request.getNotes())); transfer.getItems().clear();
        for (StockTransferLineRequest line : request.getItems()) addTransferLine(transfer, line);
        return transferResponse(transferRepository.save(transfer));
    }

    @Transactional
    public void deleteTransfer(Integer id) {
        StockTransfer transfer = transferRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Stock transfer", id));
        if (!transfer.getSourceBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
        if (!"DRAFT".equals(transfer.getStatus())) throw new ApiConflictException("TRANSFER_NOT_DELETABLE", "Only a draft transfer can be deleted");
        transferRepository.delete(transfer);
    }

    @Transactional
    public StockTransferResponse dispatchTransfer(Integer transferId, String username, String idempotencyKey) {
        String key = blankToNull(idempotencyKey);
        if (key == null) throw new BusinessRuleException("IDEMPOTENCY_KEY_REQUIRED: Idempotency-Key header is required to dispatch a transfer");
        StockTransfer transfer = transferRepository.findByIdForUpdate(transferId).orElseThrow(() -> new ResourceNotFoundException("Stock transfer", transferId));
        if (!transfer.getSourceBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
        if (key.equals(transfer.getDispatchIdempotencyKey())) return transferResponse(transfer);
        if (transfer.getDispatchIdempotencyKey() != null) throw new ApiConflictException("TRANSFER_ALREADY_DISPATCHED", "Transfer has already been dispatched");
        if (!"DRAFT".equals(transfer.getStatus())) throw new ApiConflictException("TRANSFER_NOT_DISPATCHABLE", "Only a draft transfer can be dispatched");
        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username).orElse(null);
        for (StockTransferItem item : transfer.getItems().stream().sorted(Comparator.comparing(item -> item.getSku().getId())).toList()) {
            InventoryBalance balance = balanceRepository.findByBranchIdAndSkuIdForUpdate(transfer.getSourceBranch().getId(), item.getSku().getId()).orElseThrow(() -> new ApiConflictException("INSUFFICIENT_STOCK", "No stock is available for " + item.getSku().getSkuCode()));
            if (balance.availableQuantity().compareTo(item.getRequestedQuantity()) < 0) throw new ApiConflictException("INSUFFICIENT_STOCK", "Insufficient stock for " + item.getSku().getSkuCode());
            if (item.getBatch() != null) { InventoryBatch batch = batchRepository.findByIdForUpdate(item.getBatch().getId()).orElseThrow(); if (batch.getQuantityRemaining().compareTo(item.getRequestedQuantity()) < 0) throw new ApiConflictException("INSUFFICIENT_STOCK", "Insufficient batch stock for " + item.getSku().getSkuCode()); batch.setQuantityRemaining(batch.getQuantityRemaining().subtract(item.getRequestedQuantity())); batchRepository.save(batch); }
            balance.setQuantityOnHand(balance.getQuantityOnHand().subtract(item.getRequestedQuantity())); balanceRepository.save(balance); item.setDispatchedQuantity(item.getRequestedQuantity());
            movementRepository.save(InventoryMovement.builder().branch(transfer.getSourceBranch()).sku(item.getSku()).batch(item.getBatch()).movementType(InventoryMovementType.TRANSFER_OUT).quantityDelta(item.getRequestedQuantity().negate()).unitCostSnapshot(item.getBatch() == null ? item.getSku().getDefaultCostPrice() : item.getBatch().getUnitCost()).referenceType("STOCK_TRANSFER").referenceId(transfer.getId()).performedByStaff(staff).build());
        }
        transfer.setStatus("DISPATCHED"); transfer.setDispatchIdempotencyKey(key); transfer.setDispatchedByStaff(staff); transfer.setDispatchedAt(java.time.LocalDateTime.now()); return transferResponse(transferRepository.save(transfer));
    }

    @Transactional
    public StockTransferResponse receiveTransfer(Integer transferId, ReceiveStockTransferRequest request, String username, String idempotencyKey) {
        String key = blankToNull(idempotencyKey);
        if (key == null) throw new BusinessRuleException("IDEMPOTENCY_KEY_REQUIRED: Idempotency-Key header is required to receive a transfer");
        StockTransfer transfer = transferRepository.findByIdForUpdate(transferId).orElseThrow(() -> new ResourceNotFoundException("Stock transfer", transferId));
        if (!transfer.getDestinationBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
        if (key.equals(transfer.getReceiveIdempotencyKey())) return transferResponse(transfer);
        if (transfer.getReceiveIdempotencyKey() != null) throw new ApiConflictException("TRANSFER_ALREADY_RECEIVED", "Transfer has already been received");
        if (!"DISPATCHED".equals(transfer.getStatus())) throw new ApiConflictException("TRANSFER_NOT_RECEIVABLE", "Only a dispatched transfer can be received");
        if (request.getItems().size() != transfer.getItems().size()) throw new BusinessRuleException("TRANSFER_RECEIPT_INCOMPLETE: Received quantities are required for every transfer item");
        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username).orElse(null);
        Map<Integer, ReceiveStockTransferRequest.Line> received = new java.util.HashMap<>(); for (ReceiveStockTransferRequest.Line line : request.getItems()) received.put(line.getTransferItemId(), line);
        if (received.size() != transfer.getItems().size() || transfer.getItems().stream().anyMatch(item -> !received.containsKey(item.getId()))) throw new BusinessRuleException("TRANSFER_RECEIPT_MISMATCH: Received items must match the transfer items");
        for (StockTransferItem item : transfer.getItems().stream().sorted(Comparator.comparing(item -> item.getSku().getId())).toList()) {
            ReceiveStockTransferRequest.Line line = received.get(item.getId()); if (line.getReceivedQuantity().compareTo(item.getDispatchedQuantity()) > 0) throw new BusinessRuleException("TRANSFER_RECEIPT_EXCEEDED: Received quantity cannot exceed dispatched quantity");
            if (line.getReceivedQuantity().compareTo(item.getDispatchedQuantity()) < 0 && (line.getVarianceReason() == null || line.getVarianceReason().isBlank())) throw new BusinessRuleException("TRANSFER_VARIANCE_REASON_REQUIRED: A variance reason is required");
            item.setReceivedQuantity(line.getReceivedQuantity()); item.setVarianceReason(blankToNull(line.getVarianceReason())); if (line.getReceivedQuantity().signum() == 0) continue;
            branchSkuRepository.findByBranchIdAndSkuId(transfer.getDestinationBranch().getId(), item.getSku().getId()).orElseGet(() -> branchSkuRepository.save(BranchSku.builder().branch(transfer.getDestinationBranch()).sku(item.getSku()).build()));
            InventoryBalance balance = balanceRepository.findByBranchIdAndSkuIdForUpdate(transfer.getDestinationBranch().getId(), item.getSku().getId()).orElseGet(() -> balanceRepository.saveAndFlush(InventoryBalance.builder().branch(transfer.getDestinationBranch()).sku(item.getSku()).build()));
            InventoryBatch destinationBatch = null;
            if (item.getBatch() != null) { InventoryBatch sourceBatch = item.getBatch(); destinationBatch = batchRepository.save(InventoryBatch.builder().branch(transfer.getDestinationBranch()).sku(item.getSku()).supplier(sourceBatch.getSupplier()).batchNumber(sourceBatch.getBatchNumber()).receivedAt(java.time.LocalDateTime.now()).manufacturedOn(sourceBatch.getManufacturedOn()).expiresOn(sourceBatch.getExpiresOn()).unitCost(sourceBatch.getUnitCost()).quantityReceived(line.getReceivedQuantity()).quantityRemaining(line.getReceivedQuantity()).build()); }
            balance.setQuantityOnHand(balance.getQuantityOnHand().add(line.getReceivedQuantity())); balanceRepository.save(balance);
            movementRepository.save(InventoryMovement.builder().branch(transfer.getDestinationBranch()).sku(item.getSku()).batch(destinationBatch).movementType(InventoryMovementType.TRANSFER_IN).quantityDelta(line.getReceivedQuantity()).unitCostSnapshot(destinationBatch == null ? item.getSku().getDefaultCostPrice() : destinationBatch.getUnitCost()).referenceType("STOCK_TRANSFER").referenceId(transfer.getId()).performedByStaff(staff).build());
        }
        transfer.setStatus("RECEIVED"); transfer.setReceiveIdempotencyKey(key); transfer.setReceivedByStaff(staff); transfer.setReceivedAt(java.time.LocalDateTime.now()); return transferResponse(transferRepository.save(transfer));
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> suppliers(boolean includeInactive) { return supplierRepository.findAll().stream().filter(s -> includeInactive || s.isActive()).map(this::supplierResponse).toList(); }

    private boolean isManualMovement(InventoryMovementType type) { return type == InventoryMovementType.OPENING_STOCK || type == InventoryMovementType.ADJUSTMENT_IN || type == InventoryMovementType.ADJUSTMENT_OUT || type == InventoryMovementType.WASTAGE || type == InventoryMovementType.EXPIRY; }
    private boolean isDecrease(InventoryMovementType type) { return type == InventoryMovementType.ADJUSTMENT_OUT || type == InventoryMovementType.WASTAGE || type == InventoryMovementType.EXPIRY; }
    private InventoryBalanceResponse toBalanceResponse(InventoryBalance b) {
        BranchSku config = branchSkuRepository.findByBranchIdAndSkuId(b.getBranch().getId(), b.getSku().getId()).orElse(null);
        BigDecimal reorderLevel = config == null ? BigDecimal.ZERO : config.getReorderLevel();
        BigDecimal available = b.availableQuantity();
        LocalDate nearestExpiry = batchRepository.findNearestExpiry(b.getBranch().getId(), b.getSku().getId()).orElse(null);
        return InventoryBalanceResponse.builder().skuId(b.getSku().getId()).skuCode(b.getSku().getSkuCode())
                .productName(b.getSku().getProduct().getProductName()).categoryName(b.getSku().getProduct().getCategory().getCategoryName())
                .quantityOnHand(b.getQuantityOnHand()).quantityReserved(b.getQuantityReserved()).availableQuantity(available)
                .reorderLevel(reorderLevel).lowStock(reorderLevel.signum() > 0 && available.compareTo(reorderLevel) <= 0)
                .outOfStock(available.signum() <= 0).nearestExpiry(nearestExpiry)
                .expiringSoon(nearestExpiry != null && !nearestExpiry.isAfter(LocalDate.now().plusDays(30)))
                .availableForSale(config != null && config.isAvailable() && b.getSku().isActive() && b.getSku().getProduct().isActive()).build();
    }
    private InventoryMovementResponse toMovementResponse(InventoryMovement m) { return InventoryMovementResponse.builder().id(m.getId()).skuId(m.getSku().getId()).movementType(m.getMovementType()).quantityDelta(m.getQuantityDelta()).referenceType(m.getReferenceType()).referenceId(m.getReferenceId()).reasonCode(m.getReasonCode()).notes(m.getNotes()).createdAt(m.getCreatedAt()).build(); }
    private String blankToNull(String key) { return key == null || key.isBlank() ? null : key.trim(); }
    private Pageable safePage(int page, int size) { return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)); }
    private void addTransferLine(StockTransfer transfer, StockTransferLineRequest line) {
        ProductSku sku = skuRepository.findById(line.getSkuId()).orElseThrow(() -> new ResourceNotFoundException("SKU", line.getSkuId()));
        InventoryBatch batch = line.getBatchId() == null ? null : batchRepository.findById(line.getBatchId()).orElseThrow(() -> new ResourceNotFoundException("Inventory batch", line.getBatchId()));
        if (batch != null && (!batch.getBranch().getId().equals(transfer.getSourceBranch().getId()) || !batch.getSku().getId().equals(sku.getId()))) throw new BusinessRuleException("TRANSFER_BATCH_MISMATCH: Batch must belong to the source branch and SKU");
        if (sku.isHasExpiry() && batch == null) throw new BusinessRuleException("TRANSFER_BATCH_REQUIRED: A batch is required for expiry-controlled SKUs");
        transfer.getItems().add(StockTransferItem.builder().transfer(transfer).sku(sku).batch(batch).requestedQuantity(line.getQuantity()).build());
    }
    private StockReceiptResponse receiptResponse(StockReceipt receipt) { return receiptResponse(receipt, batchRepository.findByStockReceiptIdOrderById(receipt.getId()).stream().map(InventoryBatch::getId).toList()); }
    private StockReceiptResponse receiptResponse(StockReceipt receipt, List<Integer> batchIds) { return StockReceiptResponse.builder().id(receipt.getId()).supplierId(receipt.getSupplier() == null ? null : receipt.getSupplier().getId()).supplierName(receipt.getSupplier() == null ? null : receipt.getSupplier().getSupplierName()).supplierInvoiceReference(receipt.getSupplierInvoiceReference()).receivedAt(receipt.getReceivedAt()).notes(receipt.getNotes()).batchIds(batchIds).build(); }
    private SupplierResponse supplierResponse(Supplier supplier) { return SupplierResponse.builder().id(supplier.getId()).supplierName(supplier.getSupplierName()).phone(supplier.getPhone()).email(supplier.getEmail()).taxNumber(supplier.getTaxNumber()).active(supplier.isActive()).build(); }
    private StockTransferResponse transferResponse(StockTransfer transfer) { return StockTransferResponse.builder().id(transfer.getId()).transferNumber(transfer.getTransferNumber()).sourceBranchId(transfer.getSourceBranch().getId()).destinationBranchId(transfer.getDestinationBranch().getId()).status(transfer.getStatus()).notes(transfer.getNotes()).createdAt(transfer.getCreatedAt()).dispatchedAt(transfer.getDispatchedAt()).receivedAt(transfer.getReceivedAt()).items(transfer.getItems().stream().map(item -> StockTransferResponse.Line.builder().id(item.getId()).skuId(item.getSku().getId()).skuCode(item.getSku().getSkuCode()).batchId(item.getBatch() == null ? null : item.getBatch().getId()).requestedQuantity(item.getRequestedQuantity()).dispatchedQuantity(item.getDispatchedQuantity()).receivedQuantity(item.getReceivedQuantity()).varianceReason(item.getVarianceReason()).build()).toList()).build(); }
}
