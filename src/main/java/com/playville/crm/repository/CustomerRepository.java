package com.playville.crm.repository;

import com.playville.crm.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByPhoneNumber(String phoneNumber);

    Page<Customer> findByHomeBranchIdAndIsActiveTrue(Integer branchId, Pageable pageable);

    @Query("""
        SELECT c FROM Customer c
        WHERE c.homeBranch.id = :branchId
          AND c.isActive = true
          AND LOWER(c.parentName) LIKE LOWER(CONCAT('%', :name, '%'))
        """)
    Page<Customer> searchByNameInBranch(@Param("branchId") Integer branchId,
                                        @Param("name")     String  name,
                                        Pageable pageable);

    @Query("""
        SELECT c FROM Customer c
        WHERE c.homeBranch.id = :branchId
          AND c.isActive = true
          AND c.globalSessionBalance <= :threshold
        """)
    List<Customer> findLowBalanceCustomers(@Param("branchId")  Integer branchId,
                                           @Param("threshold") int     threshold);

    boolean existsByPhoneNumber(String phoneNumber);
}