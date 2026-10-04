// RegisterRequest.java
package com.smartbank.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must be 10 to 15 digits")
        String phone,

        @NotBlank(message = "Password is required")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
                message = "Password must be 8 to 64 characters and contain at least one letter and one digit")
        String password
) {}