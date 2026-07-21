package com.playville.crm.dto.invoice;

import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.util.List;
@Getter @Setter public class ReplaceInvoiceItemsRequest { @NotNull(message = "Invoice version is required") private Long version; @Valid @NotEmpty(message = "At least one invoice item is required") private List<InvoiceLineRequest> items; }
