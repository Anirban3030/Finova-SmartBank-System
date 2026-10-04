package com.smartbank.service;

import com.smartbank.dto.PageResponse;
import com.smartbank.dto.TransactionResponse;
import com.smartbank.entity.Account;
import com.smartbank.entity.Customer;
import com.smartbank.entity.Transaction;
import com.smartbank.entity.TransactionType;
import com.smartbank.exception.BusinessRuleException;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.CustomerRepository;
import com.smartbank.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionHistoryService {

    private static final LocalDateTime EARLIEST = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime LATEST = LocalDateTime.of(2999, 1, 1, 0, 0);
    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public TransactionHistoryService(TransactionRepository transactionRepository,
                                     AccountRepository accountRepository,
                                     CustomerRepository customerRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(String email, Long accountId, TransactionType type,
                                                    LocalDate from, LocalDate to, int page, int size) {
        Customer customer = findCustomer(email);

        List<Long> accountIds;
        if (accountId != null) {
            if (!accountRepository.existsByIdAndCustomerId(accountId, customer.getId())) {
                throw new ResourceNotFoundException("Account not found");
            }
            accountIds = List.of(accountId);
        } else {
            accountIds = accountRepository.findByCustomerId(customer.getId())
                    .stream().map(Account::getId).toList();
        }

        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException("'from' date must not be after 'to' date");
        }

        LocalDateTime start = (from == null) ? EARLIEST : from.atStartOfDay();
        LocalDateTime end = (to == null) ? LATEST : to.plusDays(1).atStartOfDay();   // 'to' is inclusive
        List<TransactionType> types = (type == null) ? List.of(TransactionType.values()) : List.of(type);

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<Transaction> result = transactionRepository.search(accountIds, types, start, end, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(String email, Long id) {
        Customer customer = findCustomer(email);
        return transactionRepository.findByIdAndCustomerId(id, customer.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    private Customer findCustomer(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getAccount().getId(), t.getTransactionType(),
                t.getAmount(), t.getReference(), t.getStatus(),
                t.getTransactionDate());
    }
}