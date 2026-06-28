package com.playville.crm.repository;

import com.playville.crm.entity.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {

    Page<Purchase>  findByBranchIdOrderByCreatedAtDesc(Integer branchId, Pageable pageable);

    List<Purchase>  findByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    // Count customers whose current_package_id matches this package
    @Query("""
        SELECT COUNT(c) FROM Customer c
        WHERE c.currentPackage.id = :packageId
        """)
    long countByPlayvillePackageIdAndCustomerCurrentPackageId(
            @Param("packageId") Integer packageId);
}