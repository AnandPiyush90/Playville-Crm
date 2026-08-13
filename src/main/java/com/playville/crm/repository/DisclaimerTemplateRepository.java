package com.playville.crm.repository;

import com.playville.crm.entity.DisclaimerTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DisclaimerTemplateRepository extends JpaRepository<DisclaimerTemplate, Long> {
    List<DisclaimerTemplate> findByBranchIdOrBranchIsNull(Integer branchId);
}
