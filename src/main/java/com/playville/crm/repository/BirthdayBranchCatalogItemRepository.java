package com.playville.crm.repository;

import com.playville.crm.entity.BirthdayBranchCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BirthdayBranchCatalogItemRepository extends JpaRepository<BirthdayBranchCatalogItem, Integer> {
    List<BirthdayBranchCatalogItem> findByBranchIdOrderByCatalogItemCategoryAscCatalogItemItemNameAsc(Integer branchId);
    List<BirthdayBranchCatalogItem> findByBranchIdAndAvailableTrueAndCatalogItemActiveTrueOrderByCatalogItemCategoryAscCatalogItemItemNameAsc(Integer branchId);
    Optional<BirthdayBranchCatalogItem> findByBranchIdAndCatalogItemId(Integer branchId, Integer catalogItemId);
}
