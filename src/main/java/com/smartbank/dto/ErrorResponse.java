// ErrorResponse.java
package com.smartbank.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(LocalDateTime timestamp, int status, String message, Map<String, String> details) {}