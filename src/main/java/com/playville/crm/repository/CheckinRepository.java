package com.playville.crm.repository;

import com.playville.crm.entity.Checkin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CheckinRepository extends JpaRepository<Checkin, Integer> {

    // Active check-ins for branch dashboard
    List<Checkin> findByBranchIdAndStatus(
            Integer branchId, Checkin.CheckinStatus status);

    // Check if customer already has active check-in at any branch
    Optional<Checkin> findByCustomerIdAndStatus(
            Integer customerId, Checkin.CheckinStatus status);

    // Branch history paginated
    Page<Checkin> findByBranchIdOrderByCheckinTimeDesc(
            Integer branchId, Pageable pageable);

    // Auto-close query — all active check-ins across all branches
    @Query("""
        SELECT c FROM Checkin c
        WHERE c.status = 'Active'
          AND c.checkinTime < :cutoff
        """)
    List<Checkin> findActiveCheckinsOlderThan(
            @Param("cutoff") LocalDateTime cutoff);

    // Customer check-in history
    Page<Checkin> findByCustomerIdOrderByCheckinTimeDesc(
            Integer customerId, Pageable pageable);
}