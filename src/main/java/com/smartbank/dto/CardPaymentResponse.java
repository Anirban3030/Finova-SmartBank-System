// CardPaymentResponse.java
package com.smartbank.dto;

import com.smartbank.entity.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CardPaymentResponse(Long id, Long cardId, BigDecimal amount,
                                  TransactionStatus status, LocalDateTime paymentDate) {}