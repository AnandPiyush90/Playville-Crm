package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.config.FeatureFlagService;
import com.playville.crm.dto.purchase.*;
import com.playville.crm.entity.*;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private static final BigDecimal GST_RATE = new BigDecimal("0.18");

    private final PurchaseRepository        purchaseRepository;
    private final CustomerRepository        customerRepository;
    private final PlayvillePackageRepository packageRepository;
    private final BranchRepository          branchRepository;
    private final StaffRepository           staffRepository;
    private final CheckinRepository         checkinRepository;
    private final CustomerEntitlementRepository entitlementRepository;
    private final FeatureFlagService        featureFlags;
    private final InvoiceService            invoiceService;
    private final InvoiceRepository         invoiceRepository;

    @Transactional(readOnly = true)
    public List<PlayvillePackage> getActivePackages() {
        return packageRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Transactional
    public PurchaseResponse purchasePackage(PurchaseRequest req, String username, String idempotencyKey) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = purchaseRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                Purchase purchase = existing.get();
                invoiceService.ensurePurchaseReceipt(purchase, username);
                return toResponse(purchase, purchase.getCustomer(), purchase.getPlayvillePackage());
            }
        }

        Customer customer = customerRepository.findByIdForUpdate(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer", req.getCustomerId()));

        PlayvillePackage pkg = packageRepository.findById(req.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Package", req.getPackageId()));

        if (!pkg.isActive())
            throw new BusinessRuleException("Package is no longer available");

        Integer branchId = BranchContext.getBranchId();
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));

        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username)
                .orElse(null);

        Checkin sourceCheckin = null;
        CustomerEntitlement sourceEntitlement = null;
        if (req.getSourceCheckinId() != null || req.getSourceTrialEntitlementId() != null
                || req.getPurchaseContext() == com.playville.crm.entity.enums.PurchaseContext.TRIAL_CHECKOUT) {
            featureFlags.requireTrialConversionFlow();
        }
        if (req.getSourceCheckinId() != null) {
            sourceCheckin = checkinRepository.findById(req.getSourceCheckinId())
                    .orElseThrow(() -> new ResourceNotFoundException("Checkin", req.getSourceCheckinId()));
            if (!sourceCheckin.getCustomer().getId().equals(customer.getId()) || !sourceCheckin.getBranch().getId().equals(branchId))
                throw new BusinessRuleException("Source check-in must belong to this customer and branch");
            var existing = purchaseRepository.findBySourceCheckinId(sourceCheckin.getId());
            if (existing.isPresent()) {
                Purchase purchase = existing.get();
                invoiceService.ensurePurchaseReceipt(purchase, username);
                return toResponse(purchase, purchase.getCustomer(), purchase.getPlayvillePackage());
            }
        }
        if (req.getSourceTrialEntitlementId() != null) {
            sourceEntitlement = entitlementRepository.findById(req.getSourceTrialEntitlementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Entitlement", req.getSourceTrialEntitlementId()));
            if (!sourceEntitlement.getCustomer().getId().equals(customer.getId()))
                throw new BusinessRuleException("Source trial entitlement must belong to this customer");
        }

        BigDecimal discount = req.getDiscountApplied() != null
                ? req.getDiscountApplied() : BigDecimal.ZERO;
        BigDecimal amountAfterDiscount = pkg.getTotalPrice().subtract(discount);
        BigDecimal gst = amountAfterDiscount.multiply(GST_RATE)
                .divide(BigDecimal.ONE.add(GST_RATE), 2, RoundingMode.HALF_UP);

        int balanceBefore = customer.getGlobalSessionBalance();
        int balanceAfter  = balanceBefore + pkg.getSessionsTotal();

        Purchase purchase = Purchase.builder()
                .customer(customer)
                .branch(branch)
                .staff(staff)
                .playvillePackage(pkg)
                .sessionsAdded(pkg.getSessionsTotal())
                .amountPaid(amountAfterDiscount)
                .gstAmount(gst)
                .discountApplied(discount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .paymentMode(req.getPaymentMode())
                .paymentReference(req.getPaymentReference())
                .notes(req.getNotes())
                .idempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim())
                .sourceCheckin(sourceCheckin)
                .sourceTrialEntitlement(sourceEntitlement)
                .purchaseContext(req.getPurchaseContext() == null ? com.playville.crm.entity.enums.PurchaseContext.STANDARD : req.getPurchaseContext())
                .build();

        purchaseRepository.saveAndFlush(purchase);

        // Update customer balance and package reference
        customer.setGlobalSessionBalance(balanceAfter);
        customer.setCurrentPackage(pkg);
        customer.setPurchaseBranch(branch);
        customerRepository.save(customer);

        invoiceService.ensurePurchaseReceipt(purchase, username);

        return toResponse(purchase, customer, pkg);
    }

    @Transactional
    public Page<PurchaseResponse> getBranchPurchases(int page, int size) {
        Integer branchId = BranchContext.getBranchId();
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return purchaseRepository.findByBranchIdOrderByCreatedAtDesc(branchId, pageable)
                .map(p -> {
                    invoiceService.ensurePurchaseReceipt(p, p.getStaff() == null ? null : p.getStaff().getUsername());
                    return toResponse(p, p.getCustomer(), p.getPlayvillePackage());
                });
    }

    @Transactional
    public List<PurchaseResponse> getCustomerPurchaseHistory(Integer customerId) {
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer", customerId));
        if (!c.getHomeBranch().getId().equals(BranchContext.getBranchId()))
            throw new BranchAccessDeniedException();
        return purchaseRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(p -> {
                    invoiceService.ensurePurchaseReceipt(p, p.getStaff() == null ? null : p.getStaff().getUsername());
                    return toResponse(p, p.getCustomer(), p.getPlayvillePackage());
                })
                .toList();
    }

    private PurchaseResponse toResponse(Purchase p, Customer c,
                                         PlayvillePackage pkg) {
        Invoice invoice = invoiceRepository.findByPurchaseId(p.getId()).orElse(null);
        return PurchaseResponse.builder()
                .id(p.getId())
                .invoiceId(invoice == null ? null : invoice.getId())
                .invoiceNumber(invoice == null ? null : invoice.getInvoiceNumber())
                .customerId(c.getId())
                .parentName(c.getParentName())
                .phoneNumber(c.getPhoneNumber())
                .packageId(pkg.getId())
                .packageName(pkg.getPackageName())
                .sessionsAdded(p.getSessionsAdded())
                .amountPaid(p.getAmountPaid())
                .gstAmount(p.getGstAmount())
                .discountApplied(p.getDiscountApplied())
                .balanceBefore(p.getBalanceBefore())
                .balanceAfter(p.getBalanceAfter())
                .isUpgrade(p.isUpgrade())
                .paymentMode(p.getPaymentMode())
                .paymentReference(p.getPaymentReference())
                .sourceCheckinId(p.getSourceCheckin() == null ? null : p.getSourceCheckin().getId())
                .sourceTrialEntitlementId(p.getSourceTrialEntitlement() == null ? null : p.getSourceTrialEntitlement().getId())
                .purchaseContext(p.getPurchaseContext())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
