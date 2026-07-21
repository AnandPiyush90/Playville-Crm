package com.playville.crm.repository;

import com.playville.crm.entity.CustomerEntitlement;
import com.playville.crm.entity.enums.EntitlementStatus;
import com.playville.crm.entity.enums.EntitlementType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface CustomerEntitlementRepository extends JpaRepository<CustomerEntitlement, Integer> {
    List<CustomerEntitlement> findByCustomerId(Integer customerId);
    List<CustomerEntitlement> findByCustomerIdAndStatusAndType(Integer customerId, EntitlementStatus status, EntitlementType type);
    boolean existsByCustomerIdAndType(Integer customerId, EntitlementType type);
    List<CustomerEntitlement> findByCustomerIdAndStatus(Integer customerId, EntitlementStatus status);
    List<CustomerEntitlement> findByStatusAndExpiresAtBefore(EntitlementStatus status, java.time.LocalDateTime expiresAt);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from CustomerEntitlement e where e.id = :id")
    Optional<CustomerEntitlement> findByIdForUpdate(@Param("id") Integer id);
}
