package com.playville.crm.repository;
import com.playville.crm.entity.StockTransfer;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface StockTransferRepository extends JpaRepository<StockTransfer, Integer> {
    Optional<StockTransfer> findByIdempotencyKey(String idempotencyKey);
    @Query("select distinct t from StockTransfer t left join fetch t.items where t.id = :id and (t.sourceBranch.id = :branchId or t.destinationBranch.id = :branchId)")
    Optional<StockTransfer> findAccessibleById(@Param("id") Integer id, @Param("branchId") Integer branchId);
    @Query(value = "select t from StockTransfer t where (t.sourceBranch.id = :branchId or t.destinationBranch.id = :branchId) and (:status is null or t.status = :status) order by t.createdAt desc",
           countQuery = "select count(t) from StockTransfer t where (t.sourceBranch.id = :branchId or t.destinationBranch.id = :branchId) and (:status is null or t.status = :status)")
    Page<StockTransfer> findAccessible(@Param("branchId") Integer branchId, @Param("status") String status, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from StockTransfer t where t.id = :id") Optional<StockTransfer> findByIdForUpdate(@Param("id") Integer id);
}
