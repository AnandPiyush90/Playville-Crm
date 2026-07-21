package com.playville.crm.dto.inventory;
import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.util.List;
@Getter @Setter public class CreateStockTransferRequest { @NotNull(message = "Destination branch is required") private Integer destinationBranchId; private String notes; @Valid @NotEmpty(message = "At least one transfer item is required") private List<StockTransferLineRequest> items; }
