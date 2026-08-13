package com.playville.crm.repository;

import com.playville.crm.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate,Long> {
    Optional<EmailTemplate> findByBranchIdAndTemplateKey(Integer branchId,String templateKey);
    List<EmailTemplate> findByBranchId(Integer branchId);
    void deleteByBranchIdAndTemplateKey(Integer branchId,String templateKey);
}
