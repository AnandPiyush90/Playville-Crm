package com.playville.crm.repository;

import com.playville.crm.entity.BirthdayPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BirthdayPackageRepository extends JpaRepository<BirthdayPackage, Integer> {
    List<BirthdayPackage> findByActiveTrueOrderByBasePriceAsc();
    Optional<BirthdayPackage> findByIdAndActiveTrue(Integer id);
}
