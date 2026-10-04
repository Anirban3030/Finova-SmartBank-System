// CardResponse.java
package com.smartbank.dto;

import com.smartbank.entity.CardStatus;
import com.smartbank.entity.CardType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CardResponse(Long id, String maskedCardNumber, CardType cardType,
                           BigDecimal creditLimit, BigDecimal availableCredit,
                           BigDecimal outstandingAmount, LocalDate expiryDate, CardStatus status) {}