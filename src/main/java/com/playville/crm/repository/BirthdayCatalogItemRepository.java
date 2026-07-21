package com.playville.crm.repository;

import com.playville.crm.entity.BirthdayCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BirthdayCatalogItemRepository extends JpaRepository<BirthdayCatalogItem, Integer> {
    List<BirthdayCatalogItem> findByActiveTrueOrderByCategoryAscItemNameAsc();
    Optional<BirthdayCatalogItem> findByIdAndActiveTrue(Integer id);
    boolean existsByItemCodeIgnoreCase(String itemCode);
    boolean existsByItemCodeIgnoreCaseAndIdNot(String itemCode, Integer id);
}
