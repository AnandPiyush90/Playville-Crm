package com.playville.crm.repository;

import com.playville.crm.entity.Staff;
import com.playville.crm.entity.enums.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Integer> {
    Optional<Staff> findByUsernameAndIsActiveTrue(String username);
    Optional<Staff> findByEmailIgnoreCase(String email);
    List<Staff>     findByBranchIdAndIsActiveTrue(Integer branchId);
    List<Staff>     findByBranchIdAndRoleAndIsActiveTrue(Integer branchId, StaffRole role);
    boolean         existsByUsername(String username);
    boolean         existsByEmail(String email);
}