package com.playville.crm.repository;

import com.playville.crm.entity.BirthdayBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BirthdayBookingRepository
        extends JpaRepository<BirthdayBooking, Integer> {

    List<BirthdayBooking> findByBranchIdOrderByPartyDateAsc(Integer branchId);

    List<BirthdayBooking> findByBranchIdAndPartyDateBetweenOrderByPartyDateAsc(
            Integer branchId, LocalDate from, LocalDate to);

    // Slot conflict check
    Optional<BirthdayBooking> findByBranchIdAndPartyDateAndPartySlotStart(
            Integer branchId, LocalDate date, LocalTime slotStart);
}