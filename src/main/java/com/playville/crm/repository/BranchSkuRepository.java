package com.playville.crm.repository;

import com.playville.crm.entity.BranchSku;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BranchSkuRepository extends JpaRepository<BranchSku, Integer> {
    Optional<BranchSku> findByBranchIdAndSkuId(Integer branchId, Integer skuId);
}
