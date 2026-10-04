// CardTransactionResponse.java
package com.smartbank.dto;

import com.smartbank.entity.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CardTransactionResponse(Long id, Long cardId, String merchant, BigDecimal amount,
                                      TransactionStatus status, LocalDateTime transactionDate) {}