package com.playville.crm.repository;

import com.playville.crm.entity.DisclaimerSigningRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DisclaimerSigningRequestRepository extends JpaRepository<DisclaimerSigningRequest, Long> {
    Optional<DisclaimerSigningRequest> findByBranchIdAndIdempotencyKey(Integer branchId, String key);
    Optional<DisclaimerSigningRequest> findByTokenSha256(String tokenSha256);
}
