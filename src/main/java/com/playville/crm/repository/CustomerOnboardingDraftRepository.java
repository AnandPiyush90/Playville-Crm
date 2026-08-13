package com.playville.crm.repository;

import com.playville.crm.entity.CustomerOnboardingDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerOnboardingDraftRepository extends JpaRepository<CustomerOnboardingDraft, Long> {
    Optional<CustomerOnboardingDraft> findByBranchIdAndIdempotencyKey(Integer branchId, String key);
}
