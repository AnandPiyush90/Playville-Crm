package com.playville.crm.repository;

import com.playville.crm.entity.EmailOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from EmailOutbox o where o.status='PENDING' and o.nextAttemptAt<=:now order by o.createdAt")
    List<EmailOutbox> findDue(@Param("now") LocalDateTime now, Pageable pageable);
    java.util.Optional<EmailOutbox> findByDeliveryId(Integer deliveryId);
}
