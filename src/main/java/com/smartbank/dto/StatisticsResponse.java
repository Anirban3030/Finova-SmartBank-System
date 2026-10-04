// StatisticsResponse.java
package com.smartbank.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StatisticsResponse(List<TypeStat> transactionsByType,
                                 List<DailyStat> transactionsLast7Days,
                                 List<CountStat> accountsByType,
                                 List<CountStat> cardsByStatus) {

    public record TypeStat(String type, long count, BigDecimal totalAmount) {}
    public record DailyStat(LocalDate date, long count, BigDecimal totalAmount) {}
    public record CountStat(String label, long count) {}
}