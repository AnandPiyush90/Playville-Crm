package com.playville.crm.repository;

import com.playville.crm.entity.DisclaimerSigningRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface DisclaimerSigningRequestRepository extends JpaRepository<DisclaimerSigningRequest, Long> {
    Optional<DisclaimerSigningRequest> findByBranchIdAndIdempotencyKey(Integer branchId, String key);
    @Query("select q from DisclaimerSigningRequest q join fetch q.template join fetch q.draft where q.tokenSha256 = :tokenSha256")
    Optional<DisclaimerSigningRequest> findByTokenSha256(@Param("tokenSha256") String tokenSha256);
    @Query("select q from DisclaimerSigningRequest q join fetch q.template join fetch q.draft where q.id = :id")
    Optional<DisclaimerSigningRequest> findByIdWithTemplate(@Param("id") Long id);
}
