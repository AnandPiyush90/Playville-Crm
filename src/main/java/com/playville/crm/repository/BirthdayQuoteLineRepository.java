package com.playville.crm.repository;

import com.playville.crm.entity.BirthdayQuoteLine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BirthdayQuoteLineRepository extends JpaRepository<BirthdayQuoteLine, Integer> {
    List<BirthdayQuoteLine> findByBirthdayBookingIdOrderByLineNumberAsc(Integer birthdayBookingId);
}
