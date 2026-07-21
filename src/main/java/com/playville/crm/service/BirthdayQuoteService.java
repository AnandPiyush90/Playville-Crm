package com.playville.crm.service;

import com.playville.crm.dto.birthday.*;
import com.playville.crm.context.BranchContext;
import com.playville.crm.entity.BirthdayCatalogItem;
import com.playville.crm.entity.BirthdayDecorationPackage;
import com.playville.crm.entity.BirthdayPackage;
import com.playville.crm.entity.BirthdayBranchPolicy;
import com.playville.crm.exception.BusinessRuleException;
import com.playville.crm.exception.ResourceNotFoundException;
import com.playville.crm.repository.BirthdayCatalogItemRepository;
import com.playville.crm.repository.BirthdayPackageRepository;
import com.playville.crm.repository.BirthdayBranchCatalogItemRepository;
import com.playville.crm.repository.BranchRepository;
import com.playville.crm.repository.ProductSkuRepository;
import com.playville.crm.repository.InventoryBalanceRepository;
import com.playville.crm.entity.BirthdayBranchCatalogItem;
import com.playville.crm.entity.ProductSku;
import com.playville.crm.entity.Branch;
import com.playville.crm.exception.ApiConflictException;
import lombok.RequiredArgsConstructor;
import com.playville.crm.repository.BirthdayDecorationPackageRepository;
import com.playville.crm.repository.BirthdayBranchPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BirthdayQuoteService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private final BirthdayPackageRepository packageRepository;
    private final BirthdayCatalogItemRepository catalogRepository;
    private final BirthdayBranchCatalogItemRepository branchCatalogRepository;
    private final BranchRepository branchRepository;
    private final ProductSkuRepository skuRepository;
    private final InventoryBalanceRepository balanceRepository;

    private final BirthdayBranchPolicyRepository policyRepository;
    private final BirthdayDecorationPackageRepository decorationRepository;
    @Transactional(readOnly = true)
    public List<BirthdayPackageResponse> packages() {
        return packageRepository.findByActiveTrueOrderByBasePriceAsc().stream().map(this::packageResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BirthdayCatalogItemResponse> catalog() {
        return branchCatalogRepository.findByBranchIdAndAvailableTrueAndCatalogItemActiveTrueOrderByCatalogItemCategoryAscCatalogItemItemNameAsc(BranchContext.getBranchId())
                .stream().map(this::catalogResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BirthdayCatalogItemResponse> branchCatalog() {
        return branchCatalogRepository.findByBranchIdOrderByCatalogItemCategoryAscCatalogItemItemNameAsc(BranchContext.getBranchId())
                .stream().map(this::catalogResponse).toList();
    }

    @Transactional
    public BirthdayCatalogItemResponse createCatalogItem(BirthdayCatalogItemRequest request) {
        String code = request.getItemCode().trim().toUpperCase();
        if (catalogRepository.existsByItemCodeIgnoreCase(code)) throw new ApiConflictException("BIRTHDAY_ITEM_EXISTS", "Birthday item code already exists");
        ProductSku sku = request.getSkuId() == null ? null : skuRepository.findById(request.getSkuId()).orElseThrow(() -> new ResourceNotFoundException("SKU", request.getSkuId()));
        validateInventoryMode(request.getInventoryMode(), sku);
        BirthdayCatalogItem item = catalogRepository.save(BirthdayCatalogItem.builder().itemCode(code).category(request.getCategory())
                .sku(sku).itemName(request.getItemName().trim()).description(blankToNull(request.getDescription())).unitLabel(request.getUnitLabel().trim())
                .unitPrice(request.getUnitPrice()).taxRate(request.getTaxRate()).inventoryMode(request.getInventoryMode())
                .minimumOrderQuantity(request.getMinimumOrderQuantity()).leadTimeDays(request.getLeadTimeDays())
                .active(request.getActive() == null || request.getActive()).build());
        for (Branch branch : branchRepository.findAllByIsActiveTrue()) branchCatalogRepository.save(BirthdayBranchCatalogItem.builder()
                .branch(branch).catalogItem(item).available(true).reserveInventory(sku != null && "REQUIRED".equals(item.getInventoryMode())).build());
        return catalogResponse(branchCatalogRepository.findByBranchIdAndCatalogItemId(BranchContext.getBranchId(), item.getId()).orElseThrow());
    }

    @Transactional
    public BirthdayCatalogItemResponse updateCatalogItem(Integer id, BirthdayCatalogItemRequest request) {
        BirthdayCatalogItem item = catalogRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("BirthdayCatalogItem", id));
        String code = request.getItemCode().trim().toUpperCase();
        if (catalogRepository.existsByItemCodeIgnoreCaseAndIdNot(code, id)) throw new ApiConflictException("BIRTHDAY_ITEM_EXISTS", "Birthday item code already exists");
        ProductSku sku = request.getSkuId() == null ? null : skuRepository.findById(request.getSkuId()).orElseThrow(() -> new ResourceNotFoundException("SKU", request.getSkuId()));
        validateInventoryMode(request.getInventoryMode(), sku);
        item.setItemCode(code); item.setCategory(request.getCategory()); item.setSku(sku); item.setItemName(request.getItemName().trim());
        item.setDescription(blankToNull(request.getDescription())); item.setUnitLabel(request.getUnitLabel().trim()); item.setUnitPrice(request.getUnitPrice());
        item.setTaxRate(request.getTaxRate()); item.setInventoryMode(request.getInventoryMode()); item.setMinimumOrderQuantity(request.getMinimumOrderQuantity());
        item.setLeadTimeDays(request.getLeadTimeDays()); item.setActive(request.getActive() == null || request.getActive()); catalogRepository.save(item);
        return catalogResponse(branchCatalogRepository.findByBranchIdAndCatalogItemId(BranchContext.getBranchId(), id).orElseThrow());
    }

    @Transactional
    public BirthdayCatalogItemResponse configureBranchItem(Integer id, BirthdayBranchCatalogRequest request) {
        BirthdayCatalogItem item = catalogRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("BirthdayCatalogItem", id));
        BirthdayBranchCatalogItem config = branchCatalogRepository.findByBranchIdAndCatalogItemId(BranchContext.getBranchId(), id)
                .orElseGet(() -> BirthdayBranchCatalogItem.builder().branch(branchRepository.findById(BranchContext.getBranchId()).orElseThrow())
                        .catalogItem(item).build());
        if (Boolean.TRUE.equals(request.getReserveInventory()) && item.getSku() == null)
            throw new BusinessRuleException("SKU_REQUIRED: Link an inventory SKU before enabling reservation");
        config.setDisplayNameOverride(blankToNull(request.getDisplayNameOverride())); config.setUnitPriceOverride(request.getUnitPriceOverride());
        config.setAvailable(request.getAvailable()); config.setReserveInventory(request.getReserveInventory());
        return catalogResponse(branchCatalogRepository.save(config));
    }

    @Transactional(readOnly = true)
    public BirthdayQuotePreviewResponse preview(BirthdayQuotePreviewRequest request) {
        BirthdayPackage birthdayPackage = packageRepository.findByIdAndActiveTrue(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("BirthdayPackage", request.getPackageId()));
        List<BirthdayQuotePreviewResponse.QuoteLine> lines = new ArrayList<>();
        BirthdayBranchPolicy policy = policyRepository.findByBranchId(BranchContext.getBranchId()).orElse(null);
        BigDecimal extraKidPrice = policy == null ? birthdayPackage.getExtraKidPrice() : policy.getExtraKidPrice();
        BigDecimal extraAdultPrice = policy == null ? birthdayPackage.getExtraAdultPrice() : policy.getExtraAdultPrice();
        addLine(lines, "PLAY_PACKAGE", birthdayPackage.getPackageName(), BigDecimal.ONE, birthdayPackage.getBasePrice(), BigDecimal.ZERO);
        int extraKids = Math.max(request.getKidsCount() - birthdayPackage.getIncludedKids(), 0);
        int extraAdults = Math.max(request.getAdultsCount() - birthdayPackage.getIncludedAdults(), 0);
        if (extraKids > 0) addLine(lines, "EXTRA_KID", "Additional kids", BigDecimal.valueOf(extraKids), extraKidPrice, BigDecimal.ZERO);
        if (extraAdults > 0) addLine(lines, "EXTRA_ADULT", "Additional adults", BigDecimal.valueOf(extraAdults), extraAdultPrice, BigDecimal.ZERO);
        addFoodBox(lines, "FOOD_KIDS", "Kids box", request.getKidsFoodBox());
        addFoodBox(lines, "FOOD_ADULTS", "Adults box", request.getAdultsFoodBox());
        if (request.getDecorationPackageId() != null) {
            BirthdayDecorationPackage decoration = decorationRepository.findByIdAndBranchIdAndActiveTrue(request.getDecorationPackageId(), BranchContext.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("BirthdayDecorationPackage", request.getDecorationPackageId()));
            addLine(lines, "DECORATION", decoration.getPackageName(), BigDecimal.ONE, decoration.getPrice(), decoration.getTaxRate());
        }
        addSelections(lines, "ADD_ON", request.getAddOns());
        addSelections(lines, "EXTRA", request.getExtras());
        validateLeadTimesAndStock(request, lines);
        BigDecimal subtotal = lines.stream().map(BirthdayQuotePreviewResponse.QuoteLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxTotal = lines.stream().map(line -> line.getLineTotal().multiply(line.getTaxRate()).divide(HUNDRED, 2, RoundingMode.HALF_UP)).reduce(BigDecimal.ZERO, BigDecimal::add);
        return BirthdayQuotePreviewResponse.builder().packageId(birthdayPackage.getId()).packageName(birthdayPackage.getPackageName())
                .includedKids(birthdayPackage.getIncludedKids()).includedAdults(birthdayPackage.getIncludedAdults())
                .extraKids(extraKids).extraAdults(extraAdults).lines(lines).subtotal(scale(subtotal)).taxTotal(scale(taxTotal)).grandTotal(scale(subtotal.add(taxTotal))).build();
    }

    private void addFoodBox(List<BirthdayQuotePreviewResponse.QuoteLine> lines, String category, String label, BirthdayQuotePreviewRequest.FoodBoxRequest box) {
        if (box == null || box.getBoxCount() == null || box.getBoxCount() == 0) return;
        for (BirthdayQuotePreviewRequest.FoodComponentRequest component : safe(box.getComponents())) {
            BirthdayBranchCatalogItem offering = activeOffering(component.getCatalogItemId());
            BirthdayCatalogItem item = offering.getCatalogItem();
            requireCategory(item, "FOOD");
            BigDecimal quantity = component.getQuantityPerBox().multiply(BigDecimal.valueOf(box.getBoxCount()));
            validateOrder(item, quantity);
            addLine(lines, category, label + " - " + displayName(offering), quantity, price(offering), item.getTaxRate(), item, offering.isReserveInventory());
        }
    }

    private void addSelections(List<BirthdayQuotePreviewResponse.QuoteLine> lines, String expectedCategory, List<BirthdayQuotePreviewRequest.CatalogSelectionRequest> selections) {
        for (BirthdayQuotePreviewRequest.CatalogSelectionRequest selection : safe(selections)) {
            BirthdayBranchCatalogItem offering = activeOffering(selection.getCatalogItemId());
            BirthdayCatalogItem item = offering.getCatalogItem();
            requireCategory(item, expectedCategory);
            validateOrder(item, selection.getQuantity());
            addLine(lines, expectedCategory, displayName(offering), selection.getQuantity(), price(offering), item.getTaxRate(), item, offering.isReserveInventory());
        }
    }

    private BirthdayBranchCatalogItem activeOffering(Integer id) {
        BirthdayBranchCatalogItem offering = branchCatalogRepository.findByBranchIdAndCatalogItemId(BranchContext.getBranchId(), id)
                .orElseThrow(() -> new ResourceNotFoundException("BirthdayCatalogItem", id));
        if (!offering.isAvailable() || !offering.getCatalogItem().isActive()) throw new BusinessRuleException("BIRTHDAY_ITEM_UNAVAILABLE: Item is not available at this branch");
        return offering;
    }

    private void requireCategory(BirthdayCatalogItem item, String expected) {
        if (!expected.equals(item.getCategory())) throw new BusinessRuleException("Catalog item " + item.getItemName() + " is not a " + expected + " item");
    }

    private void addLine(List<BirthdayQuotePreviewResponse.QuoteLine> lines, String category, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxRate) {
        addLine(lines, category, description, quantity, unitPrice, taxRate, null, false);
    }
    private void addLine(List<BirthdayQuotePreviewResponse.QuoteLine> lines, String category, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxRate, BirthdayCatalogItem item, boolean reserveInventory) {
        lines.add(BirthdayQuotePreviewResponse.QuoteLine.builder().catalogItemId(item == null ? null : item.getId()).skuId(item == null || item.getSku() == null ? null : item.getSku().getId())
                .inventoryReservedOnConfirmation(reserveInventory).category(category).description(description).quantity(quantity)
                .unitPrice(scale(unitPrice)).taxRate(scale(taxRate)).lineTotal(scale(quantity.multiply(unitPrice))).build());
    }

    private <T> List<T> safe(List<T> values) { return values == null ? Collections.emptyList() : values; }
    private BigDecimal scale(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private BirthdayPackageResponse packageResponse(BirthdayPackage value) { return BirthdayPackageResponse.builder().id(value.getId()).packageCode(value.getPackageCode()).packageName(value.getPackageName()).basePrice(value.getBasePrice()).includedKids(value.getIncludedKids()).includedAdults(value.getIncludedAdults()).extraKidPrice(value.getExtraKidPrice()).extraAdultPrice(value.getExtraAdultPrice()).includedDurationMinutes(value.getIncludedDurationMinutes()).build(); }
    private BirthdayCatalogItemResponse catalogResponse(BirthdayBranchCatalogItem value) {
        BirthdayCatalogItem item = value.getCatalogItem();
        BigDecimal available = item.getSku() == null ? null : balanceRepository.findByBranchIdAndSkuId(value.getBranch().getId(), item.getSku().getId()).map(b -> b.availableQuantity()).orElse(BigDecimal.ZERO);
        return BirthdayCatalogItemResponse.builder().branchConfigurationId(value.getId()).id(item.getId()).itemCode(item.getItemCode()).category(item.getCategory())
                .itemName(displayName(value)).displayNameOverride(value.getDisplayNameOverride()).unitLabel(item.getUnitLabel()).unitPrice(price(value))
                .baseUnitPrice(item.getUnitPrice()).branchPriceOverride(value.getUnitPriceOverride()).taxRate(item.getTaxRate())
                .skuId(item.getSku() == null ? null : item.getSku().getId()).skuCode(item.getSku() == null ? null : item.getSku().getSkuCode())
                .description(item.getDescription()).inventoryMode(item.getInventoryMode()).minimumOrderQuantity(item.getMinimumOrderQuantity())
                .leadTimeDays(item.getLeadTimeDays()).active(item.isActive()).available(value.isAvailable()).reserveInventory(value.isReserveInventory())
                .availableQuantity(available).build();
    }
    private BigDecimal price(BirthdayBranchCatalogItem value) { return value.getUnitPriceOverride() == null ? value.getCatalogItem().getUnitPrice() : value.getUnitPriceOverride(); }
    private String displayName(BirthdayBranchCatalogItem value) { return value.getDisplayNameOverride() == null || value.getDisplayNameOverride().isBlank() ? value.getCatalogItem().getItemName() : value.getDisplayNameOverride(); }
    private void validateOrder(BirthdayCatalogItem item, BigDecimal quantity) {
        if (quantity.compareTo(item.getMinimumOrderQuantity()) < 0) throw new BusinessRuleException("MINIMUM_ORDER_NOT_MET: Minimum quantity for " + item.getItemName() + " is " + item.getMinimumOrderQuantity());
    }
    private void validateLeadTimesAndStock(BirthdayQuotePreviewRequest request, List<BirthdayQuotePreviewResponse.QuoteLine> lines) {
        if (request.getPartyDate() != null) {
            for (BirthdayQuotePreviewResponse.QuoteLine line : lines) if (line.getCatalogItemId() != null) {
                BirthdayCatalogItem item = catalogRepository.findById(line.getCatalogItemId()).orElseThrow();
                if (request.getPartyDate().isBefore(java.time.LocalDate.now().plusDays(item.getLeadTimeDays())))
                    throw new BusinessRuleException("LEAD_TIME_REQUIRED: " + item.getItemName() + " needs " + item.getLeadTimeDays() + " day(s) notice");
            }
        }
        java.util.Map<Integer, BigDecimal> required = new java.util.HashMap<>();
        for (BirthdayQuotePreviewResponse.QuoteLine line : lines) if (line.isInventoryReservedOnConfirmation() && line.getSkuId() != null)
            required.merge(line.getSkuId(), line.getQuantity(), BigDecimal::add);
        for (java.util.Map.Entry<Integer, BigDecimal> entry : required.entrySet()) {
            BigDecimal available = balanceRepository.findByBranchIdAndSkuId(BranchContext.getBranchId(), entry.getKey()).map(b -> b.availableQuantity()).orElse(BigDecimal.ZERO);
            if (available.compareTo(entry.getValue()) < 0) throw new ApiConflictException("BIRTHDAY_STOCK_SHORTAGE", "Only " + available + " available for selected inventory item");
        }
    }
    private void validateInventoryMode(String mode, ProductSku sku) { if (!"NONE".equals(mode) && sku == null) throw new BusinessRuleException("SKU_REQUIRED: Inventory-managed birthday items must link to a SKU"); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
