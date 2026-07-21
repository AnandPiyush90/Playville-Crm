package com.playville.crm.repository;

import com.playville.crm.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
    boolean existsByProductCode(String productCode);
    Page<Product> findByProductNameContainingIgnoreCaseOrProductCodeContainingIgnoreCase(String name, String code, Pageable pageable);
}
