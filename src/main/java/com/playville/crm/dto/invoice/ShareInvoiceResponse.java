package com.playville.crm.dto.invoice;
import lombok.Builder; import lombok.Getter;
@Getter @Builder public class ShareInvoiceResponse { private Integer deliveryId; private String channel; private String destination; private String status; }
