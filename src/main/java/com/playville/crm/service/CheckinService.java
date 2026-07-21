package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.config.FeatureFlagService;
import com.playville.crm.dto.checkin.*;
import com.playville.crm.dto.kid.KidDto;
import com.playville.crm.entity.*;
import com.playville.crm.entity.SessionDeduction.DeductionSource;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinService {

    private static final BigDecimal GST_RATE = new BigDecimal("0.18");

    private final CheckinRepository               checkinRepository;
    private final CheckinKidRepository            checkinKidRepository;
    private final CustomerRepository              customerRepository;
    private final KidRepository                   kidRepository;
    private final BranchRepository                branchRepository;
    private final StaffRepository                 staffRepository;
    private final SessionDeductionRepository      deductionRepository;
    private final InterBranchSettlementRepository settlementRepository;
    private final TrialEntitlementService          trialEntitlementService;
    private final PurchaseRepository               purchaseRepository;
    private final FeatureFlagService               featureFlags;
    private final CheckinSessionItemRepository     sessionItemRepository;
    private final ProductSkuRepository             productSkuRepository;
    private final BranchSkuRepository              branchSkuRepository;
    private final InventoryBalanceRepository       inventoryBalanceRepository;
    private final InventoryBatchRepository         inventoryBatchRepository;
    private final InventoryMovementRepository      inventoryMovementRepository;

    // ─── Check-in ────────────────────────────────────────────────

    @Transactional
    public CheckinResponse checkin(CheckinRequest req, String username) {

        Customer customer = customerRepository.findByIdForUpdate(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer", req.getCustomerId()));

        // Guard: already has active check-in
        checkinRepository.findByCustomerIdAndStatus(
                customer.getId(), Checkin.CheckinStatus.Active)
                .ifPresent(c -> { throw new ApiConflictException(
                        "ACTIVE_CHECKIN_EXISTS",
                        "Customer already has an active check-in (ID: " + c.getId() + ")"); });

        boolean complimentaryTrial = "COMPLIMENTARY_TRIAL".equals(req.getVisitType());
        if (complimentaryTrial) featureFlags.requireTrialConversionFlow();
        if (complimentaryTrial && req.getEntitlementId() == null)
            throw new BusinessRuleException("A trial entitlement is required for a complimentary trial visit");
        // Guard: sufficient balance for the existing paid flow only
        if (!complimentaryTrial && customer.getGlobalSessionBalance() < req.getKidIds().size())
            throw new BusinessRuleException(
                    "Insufficient session balance. Available: "
                    + customer.getGlobalSessionBalance()
                    + ", Required: " + req.getKidIds().size());

        Integer branchId = BranchContext.getBranchId();
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));

        // Resolve staff from username — null-safe, staff column is nullable
        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username)
                .orElse(null);

        CustomerEntitlement entitlement = complimentaryTrial
                ? trialEntitlementService.reserve(customer.getId(), req.getEntitlementId()) : null;

        Checkin checkin = Checkin.builder()
                .branch(branch)
                .customer(customer)
                .staff(staff)
                .checkinTime(LocalDateTime.now())
                .status(Checkin.CheckinStatus.Active)
                .kidsCount(req.getKidIds().size())
                .visitType(complimentaryTrial ? "COMPLIMENTARY_TRIAL" : "PAID")
                .entitlement(entitlement)
                .build();

        Checkin saved;
        try {
            saved = checkinRepository.saveAndFlush(checkin);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiConflictException("ACTIVE_CHECKIN_EXISTS", "Customer already has an active check-in");
        }

        List<KidDto> kidDtos = req.getKidIds().stream().map(kidId -> {
            Kid kid = kidRepository.findById(kidId)
                    .orElseThrow(() -> new ResourceNotFoundException("Kid", kidId));

            if (!kid.getCustomer().getId().equals(customer.getId()))
                throw new BusinessRuleException(
                        "Kid " + kidId + " does not belong to customer "
                        + customer.getId());

            if (!kid.isEligible())
                throw new BusinessRuleException(
                        kid.getKidName() + " is over 8 years old and not eligible");

            CheckinKid ck = CheckinKid.builder()
                    .checkin(saved)
                    .kid(kid)
                    .sessionUsed(false)
                    .build();
            checkinKidRepository.save(ck);

            return toKidDto(kid);
        }).toList();

        return CheckinResponse.builder()
                .checkinId(saved.getId())
                .customerId(customer.getId())
                .parentName(customer.getParentName())
                .phoneNumber(customer.getPhoneNumber())
                .branchId(branch.getId())
                .branchCode(branch.getBranchCode())
                .checkinTime(saved.getCheckinTime())
                .status(saved.getStatus().name())
                .kidsCount(saved.getKidsCount())
                .kids(kidDtos)
                .sessionBalance(customer.getGlobalSessionBalance())
                .visitType(saved.getVisitType())
                .entitlementId(entitlement != null ? entitlement.getId() : null)
                .entitlementAllocations(allocation(saved, entitlement, req.getKidIds()))
                .conversionStatus("PENDING")
                .build();
    }

    // ─── Checkout ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SessionItemResponse> sessionItems(Integer checkinId) {
        Checkin checkin = accessibleCheckin(checkinId);
        CheckinSessionItem.Status status = checkin.getStatus() == Checkin.CheckinStatus.Active
                ? CheckinSessionItem.Status.OPEN : CheckinSessionItem.Status.CHARGED;
        return sessionItemRepository.findByCheckinIdAndStatusOrderByCreatedAtAsc(checkinId, status)
                .stream().map(this::toSessionItemResponse).toList();
    }

    @Transactional
    public List<SessionItemResponse> addSessionItem(Integer checkinId, SessionItemRequest request, String username) {
        Checkin checkin = activeAccessibleCheckin(checkinId);
        ProductSku sku = productSkuRepository.findById(request.getSkuId())
                .orElseThrow(() -> new ResourceNotFoundException("SKU", request.getSkuId()));
        BranchSku branchSku = branchSkuRepository.findByBranchIdAndSkuId(checkin.getBranch().getId(), sku.getId())
                .orElseThrow(() -> new BusinessRuleException("SKU_NOT_AVAILABLE_AT_BRANCH: Item is not configured for this branch"));
        if (!branchSku.isAvailable() || !sku.isActive() || !sku.getProduct().isActive())
            throw new BusinessRuleException("PRODUCT_INACTIVE: Item is not available for sale");
        CheckinSessionItem item = sessionItemRepository.findByCheckinIdAndSkuIdAndStatus(checkinId, sku.getId(), CheckinSessionItem.Status.OPEN)
                .orElseGet(() -> CheckinSessionItem.builder().checkin(checkin).branch(checkin.getBranch()).sku(sku)
                        .addedByStaff(staffRepository.findByUsernameAndIsActiveTrue(username).orElse(null))
                        .productNameSnapshot(sku.getProduct().getProductName()).skuCodeSnapshot(sku.getSkuCode())
                        .quantity(BigDecimal.ZERO).unitPriceSnapshot(branchSku.getSalePriceOverride() == null ? sku.getDefaultSalePrice() : branchSku.getSalePriceOverride())
                        .taxRateSnapshot(sku.getTaxProfile() == null ? BigDecimal.ZERO : sku.getTaxProfile().getRatePercent())
                        .status(CheckinSessionItem.Status.OPEN).notes(request.getNotes()).build());
        if (sku.getProduct().isTrackInventory()) reserve(checkin, sku, request.getQuantity(), branchSku.isAllowNegativeStock());
        item.setQuantity(item.getQuantity().add(request.getQuantity()));
        if (request.getNotes() != null) item.setNotes(request.getNotes().trim());
        recalculate(item); sessionItemRepository.save(item);
        return openItems(checkinId);
    }

    @Transactional
    public List<SessionItemResponse> updateSessionItem(Integer checkinId, Integer itemId, SessionItemRequest request) {
        Checkin checkin = activeAccessibleCheckin(checkinId);
        CheckinSessionItem item = sessionItemRepository.findByIdAndCheckinId(itemId, checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Session item", itemId));
        if (item.getStatus() != CheckinSessionItem.Status.OPEN) throw new BusinessRuleException("SESSION_ITEM_NOT_EDITABLE: Item is already charged or removed");
        if (!item.getSku().getId().equals(request.getSkuId())) throw new BusinessRuleException("SESSION_ITEM_SKU_IMMUTABLE: Remove the line and add the correct item");
        BranchSku branchSku = branchSkuRepository.findByBranchIdAndSkuId(checkin.getBranch().getId(), item.getSku().getId())
                .orElseThrow(() -> new BusinessRuleException("SKU_NOT_AVAILABLE_AT_BRANCH: Item is not configured for this branch"));
        if (item.getSku().getProduct().isTrackInventory()) reserve(checkin, item.getSku(), request.getQuantity().subtract(item.getQuantity()), branchSku.isAllowNegativeStock());
        item.setQuantity(request.getQuantity()); item.setNotes(request.getNotes() == null ? null : request.getNotes().trim());
        recalculate(item); sessionItemRepository.save(item);
        return openItems(checkinId);
    }

    @Transactional
    public List<SessionItemResponse> removeSessionItem(Integer checkinId, Integer itemId) {
        Checkin checkin = activeAccessibleCheckin(checkinId);
        CheckinSessionItem item = sessionItemRepository.findByIdAndCheckinId(itemId, checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Session item", itemId));
        if (item.getStatus() != CheckinSessionItem.Status.OPEN) throw new BusinessRuleException("SESSION_ITEM_NOT_EDITABLE: Item is already charged or removed");
        if (item.getSku().getProduct().isTrackInventory()) reserve(checkin, item.getSku(), item.getQuantity().negate(), true);
        item.setStatus(CheckinSessionItem.Status.CANCELLED); sessionItemRepository.save(item);
        return openItems(checkinId);
    }

    @Transactional
    public CheckoutResponse checkout(Integer checkinId,
                                     CheckoutRequest req,
                                     String username) {

        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Checkin", checkinId));

        if (checkin.getStatus() == Checkin.CheckinStatus.Completed)
            return toCheckoutResponse(checkin, checkin.getCustomer(), false);
        if (checkin.getStatus() != Checkin.CheckinStatus.Active)
            throw new BusinessRuleException(
                    "Check-in is already " + checkin.getStatus().name());

        if (!checkin.getBranch().getId().equals(BranchContext.getBranchId()))
            throw new BranchAccessDeniedException();

        Customer customer = checkin.getCustomer();
        Integer  branchId = BranchContext.getBranchId();
        Branch   branch   = checkin.getBranch();

        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username)
                .orElse(null);

        com.playville.crm.entity.enums.ConversionOutcome outcome = req.getConversionOutcome() == null
                ? com.playville.crm.entity.enums.ConversionOutcome.NOT_OFFERED : req.getConversionOutcome();
        Purchase conversionPurchase = null;
        if (outcome == com.playville.crm.entity.enums.ConversionOutcome.PURCHASED) {
            if (req.getPurchaseId() == null) throw new BusinessRuleException("A purchase is required for PURCHASED outcome");
            conversionPurchase = purchaseRepository.findById(req.getPurchaseId()).orElseThrow(() -> new ResourceNotFoundException("Purchase", req.getPurchaseId()));
            if (!conversionPurchase.getCustomer().getId().equals(customer.getId()) || conversionPurchase.getSourceCheckin() == null || !conversionPurchase.getSourceCheckin().getId().equals(checkinId))
                throw new BusinessRuleException("Purchase must belong to this customer and reference this check-in");
        }
        if (outcome == com.playville.crm.entity.enums.ConversionOutcome.FOLLOW_UP_REQUIRED && req.getFollowUpAt() == null)
            throw new BusinessRuleException("Follow-up date is required for FOLLOW_UP_REQUIRED outcome");

        BigDecimal extraCharges = req.getExtraCharges() != null
                ? req.getExtraCharges() : BigDecimal.ZERO;
        BigDecimal manualGstAmount = extraCharges
                .multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        List<CheckinSessionItem> sessionItems = sessionItemRepository
                .findByCheckinIdAndStatusOrderByCreatedAtAsc(checkinId, CheckinSessionItem.Status.OPEN);
        if (!sessionItems.isEmpty())
            throw new BusinessRuleException("CHECKOUT_INVOICE_REQUIRED: Create and fully pay the visit-items invoice before checkout");
        List<CheckinSessionItem> chargedSessionItems = sessionItemRepository
                .findByCheckinIdAndStatusOrderByCreatedAtAsc(checkinId, CheckinSessionItem.Status.CHARGED);
        if (!chargedSessionItems.isEmpty()) sessionItems = chargedSessionItems;
        BigDecimal itemsTaxable = sessionItems.stream().map(CheckinSessionItem::getTaxableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal itemsTax = sessionItems.stream().map(CheckinSessionItem::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal itemsTotal = sessionItems.stream().map(CheckinSessionItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxableCharges = extraCharges.add(itemsTaxable).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gstAmount = manualGstAmount.add(itemsTax).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCharged = extraCharges.add(manualGstAmount).add(itemsTotal).setScale(2, RoundingMode.HALF_UP);

        List<CheckinKid> checkinKids = checkinKidRepository
                .findByCheckinIdAndSessionUsedFalse(checkinId);

        boolean complimentaryTrial = "COMPLIMENTARY_TRIAL".equals(checkin.getVisitType());
        if (complimentaryTrial) featureFlags.requireTrialConversionFlow();
        int balanceBefore    = customer.getGlobalSessionBalance();
        int sessionsDeducted = 0;
        boolean crossBranch  = false;

        for (CheckinKid ck : checkinKids) {
            if (complimentaryTrial) {
                ck.setSessionUsed(true);
                checkinKidRepository.save(ck);
                continue;
            }
            int before = customer.getGlobalSessionBalance();
            if (before <= 0) throw new BusinessRuleException("Insufficient session balance during checkout");
            customer.setGlobalSessionBalance(before - 1);
            int after = customer.getGlobalSessionBalance();

            ck.setSessionUsed(true);
            checkinKidRepository.save(ck);

            SessionDeduction deduction = SessionDeduction.builder()
                    .customer(customer)
                    .checkin(checkin)
                    .kid(ck.getKid())
                    .branch(branch)
                    .staff(staff)
                    .deductedAt(LocalDateTime.now())
                    .deductionSource(DeductionSource.Checkout)
                    .balanceBefore(before)
                    .balanceAfter(after)
                    .build();
            deductionRepository.save(deduction);
            sessionsDeducted++;

            // Inter-branch settlement
            if (customer.getPurchaseBranch() != null
                    && !customer.getPurchaseBranch().getId().equals(branchId)) {
                crossBranch = true;
                BigDecimal rate   = customer.getPurchaseBranch().getSettlementRate();
                BigDecimal amount = rate.divide(
                        new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                InterBranchSettlement settlement = InterBranchSettlement.builder()
                        .customer(customer)
                        .checkin(checkin)
                        .purchaseBranch(customer.getPurchaseBranch())
                        .usageBranch(branch)
                        .sessionsUsed(1)
                        .settlementRate(rate)
                        .settlementAmount(amount)
                        .build();
                settlementRepository.save(settlement);
            }
        }

        if (complimentaryTrial) trialEntitlementService.consume(checkin.getEntitlement(), checkin);

        customer.setTotalVisits(customer.getTotalVisits() + 1);
        customerRepository.save(customer);

        // OPEN items are blocked above. CHARGED items were already sold by invoice finalization.

        checkin.setStatus(Checkin.CheckinStatus.Completed);
        checkin.setCheckoutTime(LocalDateTime.now());
        checkin.setSessionsDeducted(sessionsDeducted);
        checkin.setExtraCharges(taxableCharges);
        checkin.setGstAmount(gstAmount);
        checkin.setTotalCharged(totalCharged);
        checkin.setCheckoutNotes(req.getCheckoutNotes());
        checkin.setConversionOutcome(outcome);
        checkin.setConversionReason(req.getConversionReason());
        checkin.setConversionPurchase(conversionPurchase);
        checkin.setFollowUpAt(req.getFollowUpAt());
        checkinRepository.save(checkin);

        return toCheckoutResponse(checkin, customer, crossBranch);
    }

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse checkoutPreview(Integer checkinId) {
        Checkin checkin = checkinRepository.findById(checkinId).orElseThrow(() -> new ResourceNotFoundException("Checkin", checkinId));
        if (!checkin.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
        CustomerEntitlement entitlement = checkin.getEntitlement();
        List<SessionItemResponse> items = openItems(checkinId);
        return CheckoutPreviewResponse.builder().checkinId(checkin.getId()).customerId(checkin.getCustomer().getId()).visitType(checkin.getVisitType())
                .entitlementId(entitlement == null ? null : entitlement.getId()).entitlementStatus(entitlement == null ? null : entitlement.getStatus().name())
                .entitlementExpiresAt(entitlement == null ? null : entitlement.getExpiresAt()).kidsCount(checkin.getKidsCount())
                .paidSessionBalance(checkin.getCustomer().getGlobalSessionBalance()).extraCharges(checkin.getExtraCharges())
                .sessionItems(items).itemsTaxableAmount(sum(items, SessionItemResponse::getTaxableAmount))
                .itemsTaxAmount(sum(items, SessionItemResponse::getTaxAmount)).itemsTotal(sum(items, SessionItemResponse::getLineTotal)).build();
    }

    private CheckoutResponse toCheckoutResponse(Checkin checkin, Customer customer, boolean crossBranch) {
        return CheckoutResponse.builder()
                .checkinId(checkin.getId())
                .customerId(customer.getId())
                .parentName(customer.getParentName())
                .checkinTime(checkin.getCheckinTime())
                .checkoutTime(checkin.getCheckoutTime())
                .status(checkin.getStatus().name())
                .kidsCount(checkin.getKidsCount())
                .sessionsDeducted(checkin.getSessionsDeducted())
                .balanceBefore(customer.getGlobalSessionBalance() + checkin.getSessionsDeducted())
                .balanceAfter(customer.getGlobalSessionBalance())
                .extraCharges(checkin.getExtraCharges())
                .gstAmount(checkin.getGstAmount())
                .totalCharged(checkin.getTotalCharged())
                .crossBranchSettlement(crossBranch)
                .checkoutNotes(checkin.getCheckoutNotes())
                .conversionOutcome(checkin.getConversionOutcome() == null ? null : checkin.getConversionOutcome().name())
                .conversionReason(checkin.getConversionReason() == null ? null : checkin.getConversionReason().name())
                .purchaseId(checkin.getConversionPurchase() == null ? null : checkin.getConversionPurchase().getId())
                .sessionItems(sessionItemRepository.findByCheckinIdAndStatusOrderByCreatedAtAsc(checkin.getId(), CheckinSessionItem.Status.CHARGED)
                        .stream().map(this::toSessionItemResponse).toList())
                .itemsTotal(sessionItemRepository.findByCheckinIdAndStatusOrderByCreatedAtAsc(checkin.getId(), CheckinSessionItem.Status.CHARGED)
                        .stream().map(CheckinSessionItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add))
                .build();
    }

    // ─── Queries ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CheckinResponse> getActiveCheckins() {
        Integer branchId = BranchContext.getBranchId();
        return checkinRepository
                .findByBranchIdAndStatus(branchId, Checkin.CheckinStatus.Active)
                .stream().map(this::toCheckinResponse).toList();
    }

    @Transactional(readOnly = true)
    public CheckinResponse getCheckinById(Integer checkinId) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Checkin", checkinId));
        if (!checkin.getBranch().getId().equals(BranchContext.getBranchId()))
            throw new BranchAccessDeniedException();
        return toCheckinResponse(checkin);
    }

    @Transactional
    public CheckinResponse cancel(Integer checkinId, CancelCheckinRequest request) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin", checkinId));
        if (!checkin.getBranch().getId().equals(BranchContext.getBranchId()))
            throw new BranchAccessDeniedException();
        if (checkin.getStatus() != Checkin.CheckinStatus.Active)
            throw new BusinessRuleException("Only an active check-in can be cancelled");
        for (CheckinSessionItem item : sessionItemRepository.findByCheckinIdAndStatusOrderByCreatedAtAsc(checkinId, CheckinSessionItem.Status.OPEN)) {
            if (item.getSku().getProduct().isTrackInventory()) reserve(checkin, item.getSku(), item.getQuantity().negate(), true);
            item.setStatus(CheckinSessionItem.Status.CANCELLED);
            sessionItemRepository.save(item);
        }
        if ("COMPLIMENTARY_TRIAL".equals(checkin.getVisitType()))
            trialEntitlementService.release(checkin.getEntitlement(), checkin, request.getReason());
        checkin.setStatus(Checkin.CheckinStatus.Cancelled);
        checkin.setCancellationReason(request.getReason());
        return toCheckinResponse(checkinRepository.save(checkin));
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private CheckinResponse toCheckinResponse(Checkin c) {
        List<KidDto> kids = c.getCheckinKids().stream()
                .map(ck -> toKidDto(ck.getKid())).toList();
        List<SessionItemResponse> items = c.getStatus() == Checkin.CheckinStatus.Active ? openItems(c.getId()) : List.of();
        return CheckinResponse.builder()
                .checkinId(c.getId())
                .customerId(c.getCustomer().getId())
                .parentName(c.getCustomer().getParentName())
                .phoneNumber(c.getCustomer().getPhoneNumber())
                .branchId(c.getBranch().getId())
                .branchCode(c.getBranch().getBranchCode())
                .checkinTime(c.getCheckinTime())
                .status(c.getStatus().name())
                .kidsCount(c.getKidsCount())
                .kids(kids)
                .sessionBalance(c.getCustomer().getGlobalSessionBalance())
                .visitType(c.getVisitType())
                .entitlementId(c.getEntitlement() != null ? c.getEntitlement().getId() : null)
                .entitlementAllocations(allocation(c, c.getEntitlement(),
                        c.getCheckinKids().stream().map(ck -> ck.getKid().getId()).toList()))
                .conversionStatus(c.getConversionOutcome() == null ? "PENDING" : c.getConversionOutcome().name())
                .sessionItems(items).sessionItemsTotal(sum(items, SessionItemResponse::getLineTotal))
                .build();
    }

    private List<EntitlementAllocationResponse> allocation(Checkin checkin, CustomerEntitlement entitlement, List<Integer> kidIds) {
        if (entitlement == null) return List.of();
        return List.of(EntitlementAllocationResponse.builder()
                .entitlementId(entitlement.getId())
                .entitlementType(entitlement.getType().name())
                .kidIds(kidIds)
                .sessionsReserved(entitlement.getSessionsReserved())
                .build());
    }

    private Checkin accessibleCheckin(Integer checkinId) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin", checkinId));
        if (!checkin.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
        return checkin;
    }

    private Checkin activeAccessibleCheckin(Integer checkinId) {
        Checkin checkin = accessibleCheckin(checkinId);
        if (checkin.getStatus() != Checkin.CheckinStatus.Active)
            throw new BusinessRuleException("CHECKIN_NOT_ACTIVE: Items can only be changed during an active visit");
        return checkin;
    }

    private List<SessionItemResponse> openItems(Integer checkinId) {
        return sessionItemRepository.findByCheckinIdAndStatusOrderByCreatedAtAsc(checkinId, CheckinSessionItem.Status.OPEN)
                .stream().map(this::toSessionItemResponse).toList();
    }

    private void reserve(Checkin checkin, ProductSku sku, BigDecimal delta, boolean allowNegativeStock) {
        InventoryBalance balance = inventoryBalanceRepository.findByBranchIdAndSkuIdForUpdate(checkin.getBranch().getId(), sku.getId())
                .orElseThrow(() -> new ApiConflictException("INSUFFICIENT_STOCK", "No stock is available for " + sku.getSkuCode()));
        if (delta.signum() > 0 && !allowNegativeStock && balance.availableQuantity().compareTo(delta) < 0)
            throw new ApiConflictException("INSUFFICIENT_STOCK", "Only " + balance.availableQuantity() + " available for " + sku.getProduct().getProductName());
        BigDecimal reserved = balance.getQuantityReserved().add(delta);
        if (reserved.signum() < 0) throw new IllegalStateException("Inventory reservation cannot be negative");
        balance.setQuantityReserved(reserved); inventoryBalanceRepository.save(balance);
    }

    private void recalculate(CheckinSessionItem item) {
        BigDecimal gross = item.getUnitPriceSnapshot().multiply(item.getQuantity()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal rate = item.getTaxRateSnapshot() == null ? BigDecimal.ZERO : item.getTaxRateSnapshot();
        boolean includesTax = item.getSku().getTaxProfile() == null || item.getSku().getTaxProfile().isPriceIncludesTax();
        BigDecimal taxable = includesTax && rate.signum() > 0
                ? gross.divide(BigDecimal.ONE.add(rate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)), 2, RoundingMode.HALF_UP)
                : gross;
        BigDecimal tax = includesTax ? gross.subtract(taxable) : taxable.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        item.setTaxableAmount(taxable); item.setTaxAmount(tax); item.setLineTotal(includesTax ? gross : gross.add(tax));
    }

    private void consumeSessionItem(CheckinSessionItem item, Staff staff) {
        if (!item.getSku().getProduct().isTrackInventory()) {
            item.setStatus(CheckinSessionItem.Status.CHARGED);
            sessionItemRepository.save(item);
            return;
        }
        InventoryBalance balance = inventoryBalanceRepository.findByBranchIdAndSkuIdForUpdate(item.getBranch().getId(), item.getSku().getId())
                .orElseThrow(() -> new ApiConflictException("INSUFFICIENT_STOCK", "Inventory balance is missing for " + item.getSkuCodeSnapshot()));
        if (balance.getQuantityReserved().compareTo(item.getQuantity()) < 0 || balance.getQuantityOnHand().compareTo(item.getQuantity()) < 0)
            throw new ApiConflictException("INVENTORY_RESERVATION_MISMATCH", "Reserved stock is no longer available for " + item.getProductNameSnapshot());
        balance.setQuantityReserved(balance.getQuantityReserved().subtract(item.getQuantity()));
        balance.setQuantityOnHand(balance.getQuantityOnHand().subtract(item.getQuantity()));
        inventoryBalanceRepository.save(balance);
        BigDecimal remaining = item.getQuantity();
        for (InventoryBatch batch : inventoryBatchRepository.findSellableForUpdate(item.getBranch().getId(), item.getSku().getId())) {
            if (remaining.signum() <= 0) break;
            BigDecimal allocated = batch.getQuantityRemaining().min(remaining);
            batch.setQuantityRemaining(batch.getQuantityRemaining().subtract(allocated)); inventoryBatchRepository.save(batch);
            inventoryMovementRepository.save(InventoryMovement.builder().branch(item.getBranch()).sku(item.getSku()).batch(batch)
                    .movementType(com.playville.crm.entity.enums.InventoryMovementType.SALE).quantityDelta(allocated.negate())
                    .unitCostSnapshot(batch.getUnitCost()).referenceType("CHECKIN_SESSION_ITEM").referenceId(item.getId())
                    .performedByStaff(staff).notes("Check-in #" + item.getCheckin().getId()).build());
            remaining = remaining.subtract(allocated);
        }
        if (remaining.signum() > 0) {
            if (item.getSku().isHasExpiry()) throw new ApiConflictException("BATCH_EXPIRED", "No valid batch is available for " + item.getSkuCodeSnapshot());
            inventoryMovementRepository.save(InventoryMovement.builder().branch(item.getBranch()).sku(item.getSku())
                    .movementType(com.playville.crm.entity.enums.InventoryMovementType.SALE).quantityDelta(remaining.negate())
                    .unitCostSnapshot(item.getSku().getDefaultCostPrice()).referenceType("CHECKIN_SESSION_ITEM").referenceId(item.getId())
                    .performedByStaff(staff).notes("Check-in #" + item.getCheckin().getId()).build());
        }
        item.setStatus(CheckinSessionItem.Status.CHARGED); sessionItemRepository.save(item);
    }

    private SessionItemResponse toSessionItemResponse(CheckinSessionItem item) {
        return SessionItemResponse.builder().id(item.getId()).skuId(item.getSku().getId()).productName(item.getProductNameSnapshot())
                .skuCode(item.getSkuCodeSnapshot()).quantity(item.getQuantity()).unitPrice(item.getUnitPriceSnapshot())
                .taxRate(item.getTaxRateSnapshot()).taxableAmount(item.getTaxableAmount()).taxAmount(item.getTaxAmount())
                .lineTotal(item.getLineTotal()).status(item.getStatus().name())
                .addedByStaffName(item.getAddedByStaff() == null ? null : item.getAddedByStaff().getFullName())
                .notes(item.getNotes()).addedAt(item.getCreatedAt()).build();
    }

    private BigDecimal sum(List<SessionItemResponse> items, java.util.function.Function<SessionItemResponse, BigDecimal> value) {
        return items.stream().map(value).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private KidDto toKidDto(Kid k) {
        return KidDto.builder()
                .id(k.getId())
                .kidName(k.getKidName())
                .dob(k.getDob())
                .ageInYears(k.getAgeInYears())
                .gender(k.getGender())
                .isActive(k.isActive())
                .eligible(k.isEligible())
                .build();
    }
}
