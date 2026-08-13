package com.playville.crm.repository;
import com.playville.crm.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Integer> {
    Optional<NotificationDelivery> findByBranchIdAndIdempotencyKey(Integer branchId, String idempotencyKey);
    List<NotificationDelivery> findByBranchIdOrderByCreatedAtDesc(Integer branchId, org.springframework.data.domain.Pageable pageable);
}
