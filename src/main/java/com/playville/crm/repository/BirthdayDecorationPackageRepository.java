package com.playville.crm.repository;
import com.playville.crm.entity.BirthdayDecorationPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface BirthdayDecorationPackageRepository extends JpaRepository<BirthdayDecorationPackage, Integer> {
    List<BirthdayDecorationPackage> findByBranchIdAndActiveTrueOrderByPackageNameAsc(Integer branchId);
    List<BirthdayDecorationPackage> findByBranchIdOrderByPackageNameAsc(Integer branchId);
    Optional<BirthdayDecorationPackage> findByIdAndBranchIdAndActiveTrue(Integer id, Integer branchId);
}
