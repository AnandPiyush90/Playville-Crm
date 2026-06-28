package com.playville.crm.service;

import com.playville.crm.dto.packages.*;
import com.playville.crm.entity.PlayvillePackage;
import com.playville.crm.exception.*;
import com.playville.crm.repository.PlayvillePackageRepository;
import com.playville.crm.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final PlayvillePackageRepository packageRepository;
    private final PurchaseRepository         purchaseRepository;

    @Transactional(readOnly = true)
    public List<PackageResponse> getAllActivePackages() {
        return packageRepository
                .findAllByIsActiveTrueOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PackageResponse> getAllPackages() {
        // Admin view — includes inactive
        return packageRepository
                .findAllByOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PackageResponse getById(Integer id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public PackageResponse createPackage(PackageRequest req) {
        PlayvillePackage pkg = PlayvillePackage.builder()
                .packageName(req.getPackageName())
                .sessionsPurchased(req.getSessionsPurchased())
                .sessionsBonus(req.getSessionsBonus() != null
                        ? req.getSessionsBonus() : 0)
                .pricePerSession(req.getPricePerSession())
                .totalPrice(req.getTotalPrice())
                .validityDays(req.getValidityDays())
                .birthdayDiscountPct(req.getBirthdayDiscountPct() != null
                        ? req.getBirthdayDiscountPct()
                        : java.math.BigDecimal.ZERO)
                .rechargeDiscountPct(req.getRechargeDiscountPct() != null
                        ? req.getRechargeDiscountPct()
                        : java.math.BigDecimal.ZERO)
                .canUpgrade(req.getCanUpgrade() != null
                        ? req.getCanUpgrade() : true)
                .displayOrder(req.getDisplayOrder() != null
                        ? req.getDisplayOrder() : 0)
                .build();

        return toResponse(packageRepository.save(pkg));
    }

    @Transactional
    public PackageResponse updatePackage(Integer id, PackageRequest req) {
        PlayvillePackage pkg = findOrThrow(id);

        if (req.getPackageName()        != null) pkg.setPackageName(req.getPackageName());
        if (req.getSessionsPurchased()  != null) pkg.setSessionsPurchased(req.getSessionsPurchased());
        if (req.getSessionsBonus()      != null) pkg.setSessionsBonus(req.getSessionsBonus());
        if (req.getPricePerSession()    != null) pkg.setPricePerSession(req.getPricePerSession());
        if (req.getTotalPrice()         != null) pkg.setTotalPrice(req.getTotalPrice());
        if (req.getValidityDays()       != null) pkg.setValidityDays(req.getValidityDays());
        if (req.getBirthdayDiscountPct()!= null) pkg.setBirthdayDiscountPct(req.getBirthdayDiscountPct());
        if (req.getRechargeDiscountPct()!= null) pkg.setRechargeDiscountPct(req.getRechargeDiscountPct());
        if (req.getCanUpgrade()         != null) pkg.setCanUpgrade(req.getCanUpgrade());
        if (req.getDisplayOrder()       != null) pkg.setDisplayOrder(req.getDisplayOrder());

        return toResponse(packageRepository.save(pkg));
    }

    @Transactional
    public void deactivatePackage(Integer id) {
        PlayvillePackage pkg = findOrThrow(id);

        // Guard: cannot deactivate if customers actively use it
        long activeCustomers = purchaseRepository
                .countByPlayvillePackageIdAndCustomerCurrentPackageId(id);
        if (activeCustomers > 0)
            throw new BusinessRuleException(
                    "Cannot deactivate — " + activeCustomers
                    + " customer(s) currently on this package. "
                    + "Reassign them first.");

        pkg.setActive(false);
        packageRepository.save(pkg);
    }

    @Transactional
    public PackageResponse reactivatePackage(Integer id) {
        PlayvillePackage pkg = findOrThrow(id);
        pkg.setActive(true);
        return toResponse(packageRepository.save(pkg));
    }

    private PlayvillePackage findOrThrow(Integer id) {
        return packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package", id));
    }

    private PackageResponse toResponse(PlayvillePackage p) {
        return PackageResponse.builder()
                .id(p.getId())
                .packageName(p.getPackageName())
                .sessionsPurchased(p.getSessionsPurchased())
                .sessionsBonus(p.getSessionsBonus())
                .sessionsTotal(p.getSessionsTotal())
                .pricePerSession(p.getPricePerSession())
                .totalPrice(p.getTotalPrice())
                .validityDays(p.getValidityDays())
                .birthdayDiscountPct(p.getBirthdayDiscountPct())
                .rechargeDiscountPct(p.getRechargeDiscountPct())
                .canUpgrade(p.isCanUpgrade())
                .isActive(p.isActive())
                .displayOrder(p.getDisplayOrder())
                .createdAt(p.getCreatedAt())
                .build();
    }
}