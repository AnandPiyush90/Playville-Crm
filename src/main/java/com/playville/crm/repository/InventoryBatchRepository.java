package com.playville.crm.repository;
import com.playville.crm.entity.InventoryBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Integer> {
    List<InventoryBatch> findByStockReceiptIdOrderById(Integer stockReceiptId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from InventoryBatch b where b.branch.id = :branchId and b.sku.id = :skuId and b.status = 'ACTIVE' and b.quantityRemaining > 0 and (b.expiresOn is null or b.expiresOn >= current_date) order by case when b.expiresOn is null then 1 else 0 end, b.expiresOn asc, b.receivedAt asc, b.id asc")
    List<InventoryBatch> findSellableForUpdate(@Param("branchId") Integer branchId, @Param("skuId") Integer skuId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from InventoryBatch b where b.id = :id")
    Optional<InventoryBatch> findByIdForUpdate(@Param("id") Integer id);

    @Query("select min(b.expiresOn) from InventoryBatch b where b.branch.id = :branchId and b.sku.id = :skuId and b.status = 'ACTIVE' and b.quantityRemaining > 0 and b.expiresOn is not null")
    Optional<LocalDate> findNearestExpiry(@Param("branchId") Integer branchId, @Param("skuId") Integer skuId);
}
