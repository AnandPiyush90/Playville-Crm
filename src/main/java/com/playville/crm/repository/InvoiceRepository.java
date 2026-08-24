package com.playville.crm.repository;

import com.playville.crm.entity.Invoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.time.LocalDateTime;
import com.playville.crm.entity.enums.InvoicePaymentMode;
import com.playville.crm.entity.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
    // Fetch one collection only. Fetching two List associations in one Hibernate query
    // triggers MultipleBagFetchException; payments load lazily inside the service transaction.
    @Query("select distinct i from Invoice i left join fetch i.items left join fetch i.branch left join fetch i.customer where i.id = :id")
    Optional<Invoice> findDetailById(@Param("id") Integer id);
    Optional<Invoice> findByBirthdayBookingId(Integer birthdayBookingId);
    Optional<Invoice> findFirstByCheckinIdAndStatusOrderByIdDesc(Integer checkinId, InvoiceStatus status);
    @Query("select distinct i from Invoice i join i.items item where item.purchase.id = :purchaseId")
    Optional<Invoice> findByPurchaseId(@Param("purchaseId") Integer purchaseId);
    Optional<Invoice> findByIdempotencyKey(String idempotencyKey);
    Page<Invoice> findByBranchIdOrderByCreatedAtDesc(Integer branchId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") Integer id);

    @Query(value = """
            select distinct i from Invoice i
            left join i.payments p
            where i.branch.id = :branchId
              and (:from is null or i.createdAt >= :from)
              and (:to is null or i.createdAt < :to)
              and (:status is null or i.status = :status)
              and (:customerId is null or i.customer.id = :customerId)
              and (:paymentMode is null or p.paymentMode = :paymentMode)
            order by i.createdAt desc
            """,
            countQuery = """
            select count(distinct i.id) from Invoice i
            left join i.payments p
            where i.branch.id = :branchId
              and (:from is null or i.createdAt >= :from)
              and (:to is null or i.createdAt < :to)
              and (:status is null or i.status = :status)
              and (:customerId is null or i.customer.id = :customerId)
              and (:paymentMode is null or p.paymentMode = :paymentMode)
            """)
    Page<Invoice> search(
            @Param("branchId") Integer branchId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") InvoiceStatus status,
            @Param("customerId") Integer customerId,
            @Param("paymentMode") InvoicePaymentMode paymentMode,
            Pageable pageable);
}
