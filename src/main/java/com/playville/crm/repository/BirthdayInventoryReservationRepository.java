package com.playville.crm.repository;

import com.playville.crm.entity.BirthdayInventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BirthdayInventoryReservationRepository extends JpaRepository<BirthdayInventoryReservation, Integer> {
    List<BirthdayInventoryReservation> findByBookingIdAndStatus(Integer bookingId, BirthdayInventoryReservation.Status status);
    boolean existsByBookingIdAndStatus(Integer bookingId, BirthdayInventoryReservation.Status status);
}
