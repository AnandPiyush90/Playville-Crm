package com.playville.crm.repository;

import com.playville.crm.entity.SchoolTrip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface SchoolTripRepository extends JpaRepository<SchoolTrip, Integer> {

    List<SchoolTrip> findByBranchIdOrderByTripDateAsc(Integer branchId);

    List<SchoolTrip> findByBranchIdAndTripDateBetweenOrderByTripDateAsc(
            Integer branchId, LocalDate from, LocalDate to);

    // Slot conflict check
    Optional<SchoolTrip> findByBranchIdAndTripDateAndSlotStart(
            Integer branchId, LocalDate date, LocalTime slotStart);
}