package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pv_invoice_sequences", uniqueConstraints = @UniqueConstraint(name = "uq_invoice_sequence", columnNames = {"branch_id", "financial_year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceSequence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "branch_id", nullable = false) private Branch branch;
    @Column(name = "financial_year", nullable = false, length = 9) private String financialYear;
    @Column(name = "next_number", nullable = false) @Builder.Default private Integer nextNumber = 1;
    @Version private Long version;
}
