package com.playville.crm.repository;

import com.playville.crm.entity.InvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Integer> {
    Optional<InvoicePayment> findByIdempotencyKey(String idempotencyKey);
}
