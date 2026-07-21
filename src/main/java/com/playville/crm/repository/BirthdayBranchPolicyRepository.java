package com.playville.crm.repository;
import com.playville.crm.entity.BirthdayBranchPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface BirthdayBranchPolicyRepository extends JpaRepository<BirthdayBranchPolicy, Integer> {
    Optional<BirthdayBranchPolicy> findByBranchId(Integer branchId);
}
