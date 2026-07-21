package com.playville.crm.repository;
import com.playville.crm.entity.StockReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
public interface StockReceiptRepository extends JpaRepository<StockReceipt, Integer> {
    Optional<StockReceipt> findByIdempotencyKey(String idempotencyKey);
    Optional<StockReceipt> findByIdAndBranchId(Integer id, Integer branchId);
    Page<StockReceipt> findByBranchIdOrderByReceivedAtDesc(Integer branchId, Pageable pageable);
}
