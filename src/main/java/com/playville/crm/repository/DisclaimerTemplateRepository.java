package com.playville.crm.repository;

import com.playville.crm.entity.DisclaimerTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DisclaimerTemplateRepository extends JpaRepository<DisclaimerTemplate, Long> {
    @Query("select t from DisclaimerTemplate t where t.branch.id = :branchId or t.branch is null order by t.id desc")
    List<DisclaimerTemplate> findByBranchIdOrBranchIsNull(@Param("branchId") Integer branchId);
    List<DisclaimerTemplate> findByStatus(String status);
}
