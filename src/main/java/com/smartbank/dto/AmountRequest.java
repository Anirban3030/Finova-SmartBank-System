// AmountRequest.java
package com.smartbank.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AmountRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @DecimalMax(value = "1000000.00", message = "Amount must not exceed 1,000,000.00")
        @Digits(integer = 12, fraction = 2, message = "Amount can have at most 2 decimal places")
        BigDecimal amount
) {}