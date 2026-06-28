package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
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

    @Transactional(readOnly = true)
    public List<PlayvillePackage> getActivePackages() {
        return packageRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Transactional
    public PurchaseResponse purchasePackage(PurchaseRequest req, String username) {

        Customer customer = customerRepository.findById(req.getCustomerId())
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

        BigDecimal discount = req.getDiscountApplied() != null
                ? req.getDiscountApplied() : BigDecimal.ZERO;
        BigDecimal amountAfterDiscount = pkg.getTotalPrice().subtract(discount);
        BigDecimal gst = amountAfterDiscount
                .multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);

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
                .build();

        purchaseRepository.save(purchase);

        // Update customer balance and package reference
        customer.setGlobalSessionBalance(balanceAfter);
        customer.setCurrentPackage(pkg);
        customer.setPurchaseBranch(branch);
        customerRepository.save(customer);

        return toResponse(purchase, customer, pkg);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseResponse> getBranchPurchases(int page, int size) {
        Integer branchId = BranchContext.getBranchId();
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return purchaseRepository
                .findByBranchIdOrderByCreatedAtDesc(branchId, pageable)
                .map(p -> toResponse(p, p.getCustomer(), p.getPlayvillePackage()));
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> getCustomerPurchaseHistory(Integer customerId) {
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer", customerId));
        if (!c.getHomeBranch().getId().equals(BranchContext.getBranchId()))
            throw new BranchAccessDeniedException();
        return purchaseRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(p -> toResponse(p, p.getCustomer(), p.getPlayvillePackage()))
                .toList();
    }

    private PurchaseResponse toResponse(Purchase p, Customer c,
                                         PlayvillePackage pkg) {
        return PurchaseResponse.builder()
                .id(p.getId())
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
                .createdAt(p.getCreatedAt())
                .build();
    }
}