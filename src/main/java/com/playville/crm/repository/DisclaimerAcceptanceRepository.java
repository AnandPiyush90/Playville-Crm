package com.playville.crm.repository;

import com.playville.crm.entity.DisclaimerAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DisclaimerAcceptanceRepository extends JpaRepository<DisclaimerAcceptance, Long> {
    Optional<DisclaimerAcceptance> findByRequestId(Long requestId);
    Optional<DisclaimerAcceptance> findByDraftId(Long draftId);
    List<DisclaimerAcceptance> findByCustomerIdOrderByAcceptedAtDesc(Integer customerId);
    @Query("select a from DisclaimerAcceptance a left join fetch a.customer left join fetch a.draft left join fetch a.request where a.branch.id = :branchId order by a.acceptedAt desc")
    List<DisclaimerAcceptance> findByBranchIdOrderByAcceptedAtDesc(@Param("branchId") Integer branchId);
}
