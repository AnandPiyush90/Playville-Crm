package com.playville.crm.repository;

import com.playville.crm.entity.DisclaimerAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DisclaimerAcceptanceRepository extends JpaRepository<DisclaimerAcceptance, Long> {
    Optional<DisclaimerAcceptance> findByRequestId(Long requestId);
    Optional<DisclaimerAcceptance> findByDraftId(Long draftId);
    List<DisclaimerAcceptance> findByCustomerIdOrderByAcceptedAtDesc(Integer customerId);
}
