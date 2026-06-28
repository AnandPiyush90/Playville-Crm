package com.playville.crm.repository;

import com.playville.crm.entity.SessionDeduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionDeductionRepository extends JpaRepository<SessionDeduction, Integer> {
    List<SessionDeduction> findByCheckinId(Integer checkinId);
    List<SessionDeduction> findByCustomerIdOrderByDeductedAtDesc(Integer customerId);
    List<SessionDeduction> findByBranchIdOrderByDeductedAtDesc(Integer branchId);
}