package com.playville.crm.repository;

import com.playville.crm.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Integer> {
    boolean existsByCategoryCode(String categoryCode);
    boolean existsByCategoryCodeAndIdNot(String categoryCode, Integer id);
    List<ProductCategory> findAllByIsActiveTrueOrderByDisplayOrderAscCategoryNameAsc();
    List<ProductCategory> findAllByOrderByDisplayOrderAscCategoryNameAsc();
}
