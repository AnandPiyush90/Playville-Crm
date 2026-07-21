package com.playville.crm.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime; import java.util.*;
@Entity @Table(name = "pv_stock_transfers") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockTransfer {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
 @Column(name = "transfer_number", unique = true, length = 50) private String transferNumber;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "source_branch_id", nullable = false) private Branch sourceBranch;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "destination_branch_id", nullable = false) private Branch destinationBranch;
 @Column(nullable = false, length = 20) @Builder.Default private String status = "DRAFT";
 @Column(columnDefinition = "TEXT") private String notes;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_staff_id") private Staff createdByStaff;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "dispatched_by_staff_id") private Staff dispatchedByStaff;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "received_by_staff_id") private Staff receivedByStaff;
 @Column(name = "dispatched_at") private LocalDateTime dispatchedAt; @Column(name = "received_at") private LocalDateTime receivedAt;
 @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
 @Column(name = "dispatch_idempotency_key", length = 100) private String dispatchIdempotencyKey;
 @Column(name = "receive_idempotency_key", length = 100) private String receiveIdempotencyKey;
 @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true) @Builder.Default private List<StockTransferItem> items = new ArrayList<>();
 @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt; @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
