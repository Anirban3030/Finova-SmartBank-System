// LoginRequest.java
package com.smartbank.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
        @NotBlank(message = "Email is required") @Email(message = "Email format is invalid") String email,
        @NotBlank(message = "Password is required") String password
) {}