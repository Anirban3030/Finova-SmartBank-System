// CreateAccountRequest.java
package com.smartbank.dto;

import com.smartbank.entity.AccountType;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotNull(message = "Account type is required") AccountType accountType
) {}