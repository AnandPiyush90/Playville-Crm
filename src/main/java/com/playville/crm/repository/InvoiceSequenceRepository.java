package com.playville.crm.repository;

import com.playville.crm.entity.InvoiceSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from InvoiceSequence s where s.branch.id = :branchId and s.financialYear = :financialYear")
    Optional<InvoiceSequence> findForUpdate(@Param("branchId") Integer branchId, @Param("financialYear") String financialYear);
}
