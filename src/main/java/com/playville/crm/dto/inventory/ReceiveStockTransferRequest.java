package com.playville.crm.dto.inventory;
import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal; import java.util.List;
@Getter @Setter public class ReceiveStockTransferRequest { @Valid @NotEmpty(message = "Received quantities are required") private List<Line> items; @Getter @Setter public static class Line { @NotNull(message = "Transfer item ID is required") private Integer transferItemId; @NotNull(message = "Received quantity is required") @DecimalMin(value = "0.0", message = "Received quantity cannot be negative") private BigDecimal receivedQuantity; @Size(max = 255) private String varianceReason; } }
