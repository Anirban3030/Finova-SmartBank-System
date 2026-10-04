// AccountResponse.java
package com.smartbank.dto;

import com.smartbank.entity.AccountStatus;
import com.smartbank.entity.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(Long id, String accountNumber, AccountType accountType,
                              BigDecimal balance, AccountStatus status, LocalDateTime createdAt) {}