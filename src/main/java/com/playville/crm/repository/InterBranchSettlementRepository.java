package com.playville.crm.repository;

import com.playville.crm.entity.InterBranchSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterBranchSettlementRepository
        extends JpaRepository<InterBranchSettlement, Integer> {

    List<InterBranchSettlement> findByPurchaseBranchIdAndSettledFalse(Integer branchId);
    List<InterBranchSettlement> findByUsageBranchIdAndSettledFalse(Integer branchId);
    List<InterBranchSettlement> findByCheckinId(Integer checkinId);
}