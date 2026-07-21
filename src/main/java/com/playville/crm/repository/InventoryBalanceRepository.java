package com.playville.crm.repository;

import com.playville.crm.entity.InventoryBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from InventoryBalance b where b.branch.id = :branchId and b.sku.id = :skuId")
    Optional<InventoryBalance> findByBranchIdAndSkuIdForUpdate(@Param("branchId") Integer branchId, @Param("skuId") Integer skuId);
    Optional<InventoryBalance> findByBranchIdAndSkuId(Integer branchId, Integer skuId);
    List<InventoryBalance> findByBranchIdOrderBySkuProductProductNameAsc(Integer branchId);
}
