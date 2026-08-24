package com.playville.crm.repository;

import com.playville.crm.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Integer> {
    List<Branch>     findAllByIsActiveTrue();
    Optional<Branch> findByBranchCode(String branchCode);
    boolean          existsByBranchCode(String branchCode);

    @Query("select b from Branch b left join fetch b.activeDisclaimerTemplate where b.id = :id")
    Optional<Branch> findByIdWithDisclaimerTemplate(@Param("id") Integer id);

    @Query("select b from Branch b left join fetch b.activeDisclaimerTemplate")
    List<Branch> findAllWithDisclaimerTemplate();
}