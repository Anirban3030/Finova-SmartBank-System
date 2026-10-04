package com.smartbank.service;

import com.smartbank.dto.AdminTransactionResponse;
import com.smartbank.dto.DashboardResponse;
import com.smartbank.dto.StatisticsResponse;
import com.smartbank.dto.StatisticsResponse.CountStat;
import com.smartbank.dto.StatisticsResponse.DailyStat;
import com.smartbank.dto.StatisticsResponse.TypeStat;
import com.smartbank.entity.*;
import com.smartbank.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private static final int RECENT_MAX = 50;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final CreditCardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    public AdminService(CustomerRepository customerRepository, AccountRepository accountRepository,
                        CreditCardRepository cardRepository, TransactionRepository transactionRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        LocalDate today = LocalDate.now();
        return new DashboardResponse(
                customerRepository.countByRole(Role.CUSTOMER),
                accountRepository.count(),
                money(accountRepository.sumBalances()),
                cardRepository.count(),
                money(cardRepository.sumOutstanding()),
                transactionRepository.countBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
    }

    @Transactional(readOnly = true)
    public StatisticsResponse statistics() {
        // 1. by transaction type (every type present, even with zero rows)
        Map<String, TypeStat> byType = new LinkedHashMap<>();
        for (TransactionType type : TransactionType.values()) {
            byType.put(type.name(), new TypeStat(type.name(), 0, ZERO));
        }
        for (Object[] row : transactionRepository.summaryByType()) {
            String name = ((TransactionType) row[0]).name();
            byType.put(name, new TypeStat(name, ((Number) row[1]).longValue(), money(row[2])));
        }

        // 2. last 7 days, oldest first, missing days filled with zero
        LocalDate firstDay = LocalDate.now().minusDays(6);
        Map<LocalDate, DailyStat> byDay = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = firstDay.plusDays(i);
            byDay.put(day, new DailyStat(day, 0, ZERO));
        }
        for (Object[] row : transactionRepository.dailySummarySince(firstDay.atStartOfDay())) {
            LocalDate day = LocalDate.parse(row[0].toString());       // works for java.sql.Date and LocalDate
            if (byDay.containsKey(day)) {
                byDay.put(day, new DailyStat(day, ((Number) row[1]).longValue(), money(row[2])));
            }
        }

        return new StatisticsResponse(
                List.copyOf(byType.values()),
                List.copyOf(byDay.values()),
                countByEnum(AccountType.values(), accountRepository.countGroupedByType()),
                countByEnum(CardStatus.values(), cardRepository.countGroupedByStatus()));
    }

    @Transactional(readOnly = true)
    public List<AdminTransactionResponse> recentTransactions(int limit) {
        int size = Math.min(Math.max(limit, 1), RECENT_MAX);
        return transactionRepository.findRecent(PageRequest.of(0, size))
                .stream().map(this::toResponse).toList();
    }

    // ---------- helpers ----------

    private <E extends Enum<E>> List<CountStat> countByEnum(E[] values, List<Object[]> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (E value : values) {
            counts.put(value.name(), 0L);
        }
        for (Object[] row : rows) {
            counts.put(((Enum<?>) row[0]).name(), ((Number) row[1]).longValue());
        }
        return counts.entrySet().stream().map(e -> new CountStat(e.getKey(), e.getValue())).toList();
    }

    private BigDecimal money(Object value) {
        return (value == null) ? ZERO : new BigDecimal(value.toString()).setScale(2);
    }

    private String mask(String accountNumber) {
        int visible = Math.min(4, accountNumber.length());
        return "*".repeat(accountNumber.length() - visible)
                + accountNumber.substring(accountNumber.length() - visible);
    }

    private AdminTransactionResponse toResponse(Transaction t) {
        return new AdminTransactionResponse(t.getId(), t.getTransactionType(), t.getAmount(),
                t.getReference(), t.getStatus(), t.getTransactionDate(),
                mask(t.getAccount().getAccountNumber()));
    }
}