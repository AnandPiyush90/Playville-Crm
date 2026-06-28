package com.playville.crm.repository;

import com.playville.crm.entity.Kid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KidRepository extends JpaRepository<Kid, Integer> {

    List<Kid> findByCustomerIdAndIsActiveTrue(Integer customerId);

    List<Kid> findByBranchIdAndIsActiveTrue(Integer branchId);

    @Query("""
        SELECT k FROM Kid k
        WHERE k.customer.id = :customerId
          AND k.isActive = true
          AND TIMESTAMPDIFF(YEAR, k.dob, CURRENT_DATE) <= 8
        """)
    List<Kid> findEligibleKidsByCustomer(@Param("customerId") Integer customerId);
}