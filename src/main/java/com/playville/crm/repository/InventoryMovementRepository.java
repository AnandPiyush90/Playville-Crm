package com.playville.crm.repository;

import com.playville.crm.entity.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.time.LocalDateTime;
import com.playville.crm.entity.enums.InventoryMovementType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Integer> {
    Page<InventoryMovement> findByBranchIdAndSkuIdOrderByCreatedAtDesc(Integer branchId, Integer skuId, Pageable pageable);
    Optional<InventoryMovement> findByIdempotencyKey(String idempotencyKey);

    @Query("select m from InventoryMovement m where m.branch.id = :branchId and m.sku.id = :skuId "
            + "and (:from is null or m.createdAt >= :from) and (:to is null or m.createdAt < :to) "
            + "and (:type is null or m.movementType = :type) order by m.createdAt desc")
    Page<InventoryMovement> search(
            @Param("branchId") Integer branchId,
            @Param("skuId") Integer skuId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("type") InventoryMovementType type,
            Pageable pageable);
}
