// BeneficiaryRequest.java
package com.smartbank.dto;

import jakarta.validation.constraints.*;

public record BeneficiaryRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Account number is required")
        @Pattern(regexp = "^[0-9]{9,18}$", message = "Account number must be 9 to 18 digits")
        String accountNumber,

        @NotBlank(message = "Bank name is required")
        @Size(max = 100, message = "Bank name must be at most 100 characters")
        String bankName
) {}