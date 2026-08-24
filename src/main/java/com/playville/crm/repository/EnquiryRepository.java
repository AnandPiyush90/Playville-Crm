package com.playville.crm.repository;
import com.playville.crm.entity.Enquiry;
import com.playville.crm.entity.enums.EnquiryStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EnquiryRepository extends JpaRepository<Enquiry, Integer> {
    Optional<Enquiry> findByIdempotencyKey(String idempotencyKey);

    Page<Enquiry> findByBranchIdAndStatusOrderByCreatedAtDesc(Integer branchId, EnquiryStatus status, Pageable pageable);
    Page<Enquiry> findByBranchIdOrderByCreatedAtDesc(Integer branchId, Pageable pageable);
    @Query("""
        select e from Enquiry e
        where e.branch.id = :branchId
          and (:status is null or e.status = :status)
          and (
            lower(e.parentName) like lower(concat('%', :search, '%'))
            or e.phoneNumber like concat('%', :search, '%')
          )
        order by e.createdAt desc
        """)
    Page<Enquiry> search(@Param("branchId") Integer branchId,
                         @Param("status") EnquiryStatus status,
                         @Param("search") String search,
                         Pageable pageable);
}
