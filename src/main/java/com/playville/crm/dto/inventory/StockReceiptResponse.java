package com.playville.crm.dto.inventory;

import lombok.Builder; import lombok.Getter;
import java.time.LocalDateTime; import java.util.List;
@Getter @Builder public class StockReceiptResponse { private Integer id; private Integer supplierId; private String supplierName; private String supplierInvoiceReference; private LocalDateTime receivedAt; private String notes; private List<Integer> batchIds; }
