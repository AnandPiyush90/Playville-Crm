package com.playville.crm.repository;

import com.playville.crm.entity.PlayvillePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayvillePackageRepository
        extends JpaRepository<PlayvillePackage, Integer> {

    List<PlayvillePackage> findAllByIsActiveTrueOrderByDisplayOrderAsc();

    List<PlayvillePackage> findAllByOrderByDisplayOrderAsc();  // ← add this
}