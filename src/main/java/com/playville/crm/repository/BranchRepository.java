package com.playville.crm.repository;

import com.playville.crm.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Integer> {
    List<Branch>     findAllByIsActiveTrue();
    Optional<Branch> findByBranchCode(String branchCode);
    boolean          existsByBranchCode(String branchCode);
}