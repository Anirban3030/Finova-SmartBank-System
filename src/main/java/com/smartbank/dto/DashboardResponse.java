// DashboardResponse.java
package com.smartbank.dto;

import java.math.BigDecimal;

public record DashboardResponse(long totalCustomers, long totalAccounts, BigDecimal totalBalance,
                                long totalCards, BigDecimal totalCardOutstanding,
                                long transactionsToday) {}