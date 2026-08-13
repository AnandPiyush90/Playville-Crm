package com.playville.crm.repository;

import com.playville.crm.entity.EmailProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailProviderConfigRepository extends JpaRepository<EmailProviderConfig, Long> {
    Optional<EmailProviderConfig> findByBranchId(Integer branchId);
}
