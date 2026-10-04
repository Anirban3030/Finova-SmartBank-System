package com.smartbank.service;

import com.smartbank.dto.TransactionResponse;
import com.smartbank.entity.*;
import com.smartbank.exception.BusinessRuleException;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.CustomerRepository;
import com.smartbank.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository,
                              CustomerRepository customerRepository,
                              TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse deposit(String email, Long accountId, BigDecimal amount) {
        Account account = lockOwnedActiveAccount(email, accountId);
        BigDecimal value = amount.setScale(2);

        account.setBalance(account.getBalance().add(value));
        return toResponse(record(account, TransactionType.DEPOSIT, value, newReference()));
    }

    @Transactional
    public TransactionResponse withdraw(String email, Long accountId, BigDecimal amount) {
        Account account = lockOwnedActiveAccount(email, accountId);
        BigDecimal value = amount.setScale(2);

        if (account.getBalance().compareTo(value) < 0) {
            throw new BusinessRuleException("Insufficient balance");
        }
        account.setBalance(account.getBalance().subtract(value));
        return toResponse(record(account, TransactionType.WITHDRAWAL, value, newReference()));
    }

    @Transactional
    public TransactionResponse transfer(String email, Long sourceId, String toAccountNumber, BigDecimal amount) {
        Customer customer = findCustomer(email);

        // Checks that load ids only, never Account entities, so nothing stale ends up in memory
        if (!accountRepository.existsByIdAndCustomerId(sourceId, customer.getId())) {
            throw new ResourceNotFoundException("Account not found");
        }
        Long destId = accountRepository.findIdByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));
        if (destId.equals(sourceId)) {
            throw new BusinessRuleException("Cannot transfer to the same account");
        }

        // Lock both rows in ascending id order, so two opposite transfers can never deadlock
        Account first = lockById(Math.min(sourceId, destId));
        Account second = lockById(Math.max(sourceId, destId));
        Account source = first.getId().equals(sourceId) ? first : second;
        Account destination = (source == first) ? second : first;

        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Account is not active");
        }
        if (destination.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Destination account is not active");
        }

        BigDecimal value = amount.setScale(2);
        if (source.getBalance().compareTo(value) < 0) {
            throw new BusinessRuleException("Insufficient balance");
        }

        source.setBalance(source.getBalance().subtract(value));
        destination.setBalance(destination.getBalance().add(value));

        String reference = newReference();                       // shared by both sides
        Transaction debit = record(source, TransactionType.TRANSFER_DEBIT, value, reference);
        record(destination, TransactionType.TRANSFER_CREDIT, value, reference);
        return toResponse(debit);
    }

    // ---------- helpers ----------

    private Customer findCustomer(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    /** Finds MY account, locks its row until this transaction ends, and requires ACTIVE status. */
    private Account lockOwnedActiveAccount(String email, Long accountId) {
        Customer customer = findCustomer(email);

        Account account = accountRepository.findByIdAndCustomerIdForUpdate(accountId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Account is not active");
        }
        return account;
    }

    private Account lockById(Long id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private String newReference() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private Transaction record(Account account, TransactionType type, BigDecimal amount, String reference) {
        Transaction t = new Transaction();
        t.setAccount(account);
        t.setTransactionType(type);
        t.setAmount(amount);
        t.setReference(reference);
        return transactionRepository.save(t);      // status defaults to SUCCESS
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getAccount().getId(), t.getTransactionType(),
                t.getAmount(), t.getReference(), t.getStatus(),
                t.getTransactionDate());
    }
}