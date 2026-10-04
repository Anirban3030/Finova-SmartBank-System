// TransactionResponse.java
package com.smartbank.dto;

import com.smartbank.entity.TransactionStatus;
import com.smartbank.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(Long id, Long accountId, TransactionType transactionType,
                                  BigDecimal amount, String reference, TransactionStatus status,
                                  LocalDateTime transactionDate) {}