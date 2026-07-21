package com.playville.crm.dto.invoice;

import jakarta.validation.Valid;
import lombok.Getter; import lombok.Setter;
import java.util.List;

@Getter @Setter
public class CreateInvoiceRequest {
    private Integer customerId;
    private Integer checkinId;
    private String notes;
    @Valid private List<InvoiceLineRequest> items;
}
