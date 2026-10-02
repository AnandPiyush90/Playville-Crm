package com.playville.crm.repository;

import com.playville.crm.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByPhoneNumber(String phoneNumber);

    @Query("""
        SELECT DISTINCT c FROM Customer c
        LEFT JOIN FETCH c.homeBranch
        LEFT JOIN FETCH c.currentPackage
        LEFT JOIN FETCH c.kids
        WHERE c.id = :id
        """)
    Optional<Customer> findByIdWithDetails(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id = :id")
    Optional<Customer> findByIdForUpdate(@Param("id") Integer id);

    Page<Customer> findByHomeBranchIdAndIsActiveTrue(Integer branchId, Pageable pageable);

    @Query("""
        SELECT c FROM Customer c
        WHERE c.homeBranch.id = :branchId
          AND c.isActive = true
          AND (
              LOWER(c.parentName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR c.phoneNumber LIKE CONCAT('%', :search, '%')
          )
        """)
    Page<Customer> searchByNameOrPhoneInBranch(@Param("branchId") Integer branchId,
                                                @Param("search")   String  search,
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
