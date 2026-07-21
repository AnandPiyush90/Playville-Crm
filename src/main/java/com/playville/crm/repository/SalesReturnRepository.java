package com.playville.crm.repository;
import com.playville.crm.entity.SalesReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SalesReturnRepository extends JpaRepository<SalesReturn, Integer> { Optional<SalesReturn> findByIdempotencyKey(String idempotencyKey); }
