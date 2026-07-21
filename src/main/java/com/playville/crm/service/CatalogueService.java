package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.catalogue.*;
import com.playville.crm.entity.*;
import com.playville.crm.entity.enums.ProductType;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CatalogueService {
    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductSkuRepository skuRepository;
    private final PlayvillePackageRepository packageRepository;
    private final TaxProfileRepository taxProfileRepository;
    private final BranchSkuRepository branchSkuRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final BranchRepository branchRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String code = request.getCategoryCode().trim().toUpperCase(Locale.ROOT);
        if (categoryRepository.existsByCategoryCode(code))
            throw new ApiConflictException("CATEGORY_CODE_EXISTS", "A category with this code already exists");
        ProductCategory parent = request.getParentId() == null ? null : categoryRepository.findById(request.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Product category", request.getParentId()));
        ProductCategory category = categoryRepository.save(ProductCategory.builder()
                .categoryCode(code).categoryName(request.getCategoryName().trim()).parent(parent)
                .displayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder()).build());
        return toCategoryResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> categories(boolean includeInactive) {
        return (includeInactive ? categoryRepository.findAllByOrderByDisplayOrderAscCategoryNameAsc()
                : categoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAscCategoryNameAsc()).stream()
                .map(this::toCategoryResponse).toList();
    }

    @Transactional
    public CategoryResponse updateCategory(Integer id, CreateCategoryRequest request) {
        ProductCategory category = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product category", id));
        String code = request.getCategoryCode().trim().toUpperCase(Locale.ROOT);
        if (categoryRepository.existsByCategoryCodeAndIdNot(code, id))
            throw new ApiConflictException("CATEGORY_CODE_EXISTS", "A category with this code already exists");
        ProductCategory parent = request.getParentId() == null ? null : categoryRepository.findById(request.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Product category", request.getParentId()));
        if (parent != null && parent.getId().equals(id)) throw new BusinessRuleException("INVALID_CATEGORY_PARENT: A category cannot be its own parent");
        category.setCategoryCode(code); category.setCategoryName(request.getCategoryName().trim()); category.setParent(parent);
        category.setDisplayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder());
        return toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse setCategoryActive(Integer id, boolean active) {
        ProductCategory category = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product category", id));
        category.setActive(active);
        return toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        String code = request.getProductCode().trim().toUpperCase(Locale.ROOT);
        if (productRepository.existsByProductCode(code))
            throw new ApiConflictException("PRODUCT_CODE_EXISTS", "A product with this code already exists");
        validateSkuCodes(request.getSkus());
        ProductCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Product category", request.getCategoryId()));
        if (!category.isActive()) throw new BusinessRuleException("PRODUCT_CATEGORY_INACTIVE: Product category is not active");
        PlayvillePackage pkg = null;
        if (request.getProductType() == ProductType.MEMBERSHIP) {
            if (request.getPackageId() == null) throw new BusinessRuleException("PACKAGE_REQUIRED: A package is required for a membership product");
            pkg = packageRepository.findById(request.getPackageId()).orElseThrow(() -> new ResourceNotFoundException("Package", request.getPackageId()));
        } else if (request.getPackageId() != null) {
            throw new BusinessRuleException("PACKAGE_NOT_ALLOWED: Only membership products may reference a package");
        }
        boolean trackInventory = request.getTrackInventory() == null ? request.getProductType() == ProductType.RETAIL : request.getTrackInventory();
        if (request.getProductType() == ProductType.MEMBERSHIP && trackInventory)
            throw new BusinessRuleException("INVALID_PRODUCT_CONFIGURATION: Membership products cannot track inventory");
        Product product = productRepository.save(Product.builder().productCode(code).productName(request.getProductName().trim())
                .description(request.getDescription()).category(category).productType(request.getProductType())
                .playvillePackage(pkg).trackInventory(trackInventory).build());
        for (SkuRequest skuRequest : request.getSkus()) createSku(product, skuRequest);
        return getProduct(product.getId());
    }

    @Transactional
    public ProductResponse updateProduct(Integer productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        ProductCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Product category", request.getCategoryId()));
        if (!category.isActive()) throw new BusinessRuleException("PRODUCT_CATEGORY_INACTIVE: Product category is not active");
        product.setProductName(request.getProductName().trim());
        product.setDescription(blankToNull(request.getDescription()));
        product.setCategory(category);
        if (request.getTrackInventory() != null) product.setTrackInventory(request.getTrackInventory());
        List<ProductSku> existing = skuRepository.findByProductIdOrderByIdAsc(productId);
        Set<Integer> expectedIds = existing.stream().map(ProductSku::getId).collect(java.util.stream.Collectors.toSet());
        Set<Integer> submittedIds = request.getSkus().stream().map(UpdateSkuRequest::getId).collect(java.util.stream.Collectors.toSet());
        if (!expectedIds.equals(submittedIds)) throw new BusinessRuleException("SKU_SET_MISMATCH: Existing SKUs cannot be removed from product history; deactivate them instead");
        for (UpdateSkuRequest value : request.getSkus()) {
            ProductSku sku = existing.stream().filter(row -> row.getId().equals(value.getId())).findFirst()
                    .orElseThrow(() -> new BusinessRuleException("SKU_PRODUCT_MISMATCH: SKU does not belong to this product"));
            String skuCode = value.getSkuCode().trim().toUpperCase(Locale.ROOT);
            String barcode = blankToNull(value.getBarcode());
            if (skuRepository.existsBySkuCodeAndIdNot(skuCode, sku.getId())) throw new ApiConflictException("SKU_CODE_EXISTS", "A SKU with this code already exists");
            if (barcode != null && skuRepository.existsByBarcodeAndIdNot(barcode, sku.getId())) throw new ApiConflictException("BARCODE_EXISTS", "A SKU with this barcode already exists");
            TaxProfile tax = value.getTaxProfileId() == null ? null : taxProfileRepository.findById(value.getTaxProfileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tax profile", value.getTaxProfileId()));
            sku.setSkuCode(skuCode); sku.setBarcode(barcode); sku.setVariantAttributesJson(blankToNull(value.getVariantAttributesJson()));
            sku.setUnitOfMeasure(value.getUnitOfMeasure().trim().toUpperCase(Locale.ROOT)); sku.setDefaultSalePrice(value.getDefaultSalePrice());
            sku.setDefaultCostPrice(value.getDefaultCostPrice()); sku.setTaxProfile(tax); sku.setHasExpiry(Boolean.TRUE.equals(value.getHasExpiry()));
            if (value.getActive() != null) sku.setActive(value.getActive());
            skuRepository.save(sku);
        }
        productRepository.save(product);
        return getProduct(productId);
    }

    @Transactional
    public ProductResponse setProductActive(Integer productId, boolean active) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        product.setActive(active);
        productRepository.save(product);
        for (ProductSku sku : skuRepository.findByProductIdOrderByIdAsc(productId)) { sku.setActive(active); skuRepository.save(sku); }
        return getProduct(productId);
    }

    @Transactional(readOnly = true)
    public List<TaxProfileResponse> activeTaxProfiles() {
        return taxProfileRepository.findAll().stream().filter(TaxProfile::isActive)
                .map(t -> TaxProfileResponse.builder().id(t.getId()).taxCode(t.getTaxCode()).taxName(t.getTaxName())
                        .hsnSacCode(t.getHsnSacCode()).ratePercent(t.getRatePercent()).priceIncludesTax(t.isPriceIncludesTax()).build()).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Integer productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        return toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> products(String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("productName").ascending());
        Page<Product> products = search == null || search.isBlank() ? productRepository.findAll(pageable)
                : productRepository.findByProductNameContainingIgnoreCaseOrProductCodeContainingIgnoreCase(search.trim(), search.trim(), pageable);
        return products.map(this::toProductResponse);
    }

    @Transactional
    public SkuResponse configureCurrentBranchSku(Integer skuId, UpdateBranchSkuRequest request) {
        Integer branchId = BranchContext.getBranchId();
        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        ProductSku sku = skuRepository.findById(skuId).orElseThrow(() -> new ResourceNotFoundException("SKU", skuId));
        BranchSku branchSku = branchSkuRepository.findByBranchIdAndSkuId(branchId, skuId).orElseGet(() -> BranchSku.builder().branch(branch).sku(sku).build());
        if (request.getSalePriceOverride() != null) branchSku.setSalePriceOverride(request.getSalePriceOverride());
        if (request.getReorderLevel() != null) branchSku.setReorderLevel(request.getReorderLevel());
        if (request.getReorderQuantity() != null) branchSku.setReorderQuantity(request.getReorderQuantity());
        if (request.getIsAvailable() != null) branchSku.setAvailable(request.getIsAvailable());
        branchSkuRepository.save(branchSku);
        InventoryBalance balance = balanceRepository.findByBranchIdAndSkuId(branchId, skuId).orElse(null);
        return SkuResponse.builder().id(sku.getId()).skuCode(sku.getSkuCode()).barcode(sku.getBarcode()).variantAttributesJson(sku.getVariantAttributesJson()).unitOfMeasure(sku.getUnitOfMeasure()).salePrice(branchSku.getSalePriceOverride() == null ? sku.getDefaultSalePrice() : branchSku.getSalePriceOverride()).defaultSalePrice(sku.getDefaultSalePrice()).defaultCostPrice(sku.getDefaultCostPrice()).taxProfileId(sku.getTaxProfile() == null ? null : sku.getTaxProfile().getId()).availableQuantity(balance == null ? BigDecimal.ZERO : balance.availableQuantity()).availableAtBranch(branchSku.isAvailable()).hasExpiry(sku.isHasExpiry()).active(sku.isActive()).build();
    }

    private void createSku(Product product, SkuRequest request) {
        String skuCode = request.getSkuCode().trim().toUpperCase(Locale.ROOT);
        if (skuRepository.existsBySkuCode(skuCode)) throw new ApiConflictException("SKU_CODE_EXISTS", "A SKU with this code already exists");
        String barcode = blankToNull(request.getBarcode());
        if (barcode != null && skuRepository.existsByBarcode(barcode)) throw new ApiConflictException("BARCODE_EXISTS", "A SKU with this barcode already exists");
        TaxProfile taxProfile = request.getTaxProfileId() == null ? null : taxProfileRepository.findById(request.getTaxProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Tax profile", request.getTaxProfileId()));
        skuRepository.save(ProductSku.builder().product(product).skuCode(skuCode).barcode(barcode)
                .variantAttributesJson(blankToNull(request.getVariantAttributesJson()))
                .unitOfMeasure(request.getUnitOfMeasure().trim().toUpperCase(Locale.ROOT))
                .defaultSalePrice(request.getDefaultSalePrice()).defaultCostPrice(request.getDefaultCostPrice())
                .taxProfile(taxProfile).hasExpiry(Boolean.TRUE.equals(request.getHasExpiry())).build());
    }

    private void validateSkuCodes(List<SkuRequest> skus) {
        Set<String> codes = new HashSet<>();
        for (SkuRequest sku : skus) if (!codes.add(sku.getSkuCode().trim().toUpperCase(Locale.ROOT)))
            throw new BusinessRuleException("DUPLICATE_SKU_IN_REQUEST: SKU codes must be unique within a product");
    }

    private ProductResponse toProductResponse(Product product) {
        Integer branchId = BranchContext.getBranchId();
        List<SkuResponse> skus = skuRepository.findAll().stream().filter(s -> s.getProduct().getId().equals(product.getId()))
                .map(s -> {
                    BranchSku branchSku = branchSkuRepository.findByBranchIdAndSkuId(branchId, s.getId()).orElse(null);
                    InventoryBalance balance = balanceRepository.findByBranchIdAndSkuId(branchId, s.getId()).orElse(null);
                    BigDecimal price = branchSku != null && branchSku.getSalePriceOverride() != null ? branchSku.getSalePriceOverride() : s.getDefaultSalePrice();
                    return SkuResponse.builder().id(s.getId()).skuCode(s.getSkuCode()).barcode(s.getBarcode())
                            .variantAttributesJson(s.getVariantAttributesJson()).unitOfMeasure(s.getUnitOfMeasure()).salePrice(price)
                            .defaultSalePrice(s.getDefaultSalePrice()).defaultCostPrice(s.getDefaultCostPrice()).taxProfileId(s.getTaxProfile() == null ? null : s.getTaxProfile().getId())
                            .availableQuantity(balance == null ? BigDecimal.ZERO : balance.availableQuantity())
                            .availableAtBranch(branchSku != null && branchSku.isAvailable()).hasExpiry(s.isHasExpiry()).active(s.isActive()).build();
                }).toList();
        return ProductResponse.builder().id(product.getId()).productCode(product.getProductCode()).productName(product.getProductName())
                .description(product.getDescription()).categoryId(product.getCategory().getId()).categoryName(product.getCategory().getCategoryName())
                .productType(product.getProductType()).packageId(product.getPlayvillePackage() == null ? null : product.getPlayvillePackage().getId())
                .trackInventory(product.isTrackInventory()).active(product.isActive()).skus(skus).build();
    }

    private CategoryResponse toCategoryResponse(ProductCategory category) { return CategoryResponse.builder().id(category.getId()).parentId(category.getParent() == null ? null : category.getParent().getId()).categoryCode(category.getCategoryCode()).categoryName(category.getCategoryName()).displayOrder(category.getDisplayOrder()).active(category.isActive()).build(); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
