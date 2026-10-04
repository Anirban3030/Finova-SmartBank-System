// AskRequest.java
package com.smartbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @NotBlank(message = "Question is required")
        @Size(max = 1000, message = "Question must be at most 1000 characters")
        String question
) {}