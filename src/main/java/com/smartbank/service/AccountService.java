package com.smartbank.service;

import com.smartbank.dto.AccountResponse;
import com.smartbank.entity.Account;
import com.smartbank.entity.AccountType;
import com.smartbank.entity.Customer;
import com.smartbank.exception.DuplicateResourceException;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountService(AccountRepository accountRepository, CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    /** Used at registration. Joins the caller's transaction, so it rolls back with it. */
    @Transactional
    public Account createAccount(Customer customer, AccountType type) {
        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountType(type);
        account.setAccountNumber(generateAccountNumber());
        account.setBalance(BigDecimal.ZERO.setScale(2));
        return accountRepository.save(account);          // status defaults to ACTIVE
    }

    @Transactional
    public AccountResponse openAccount(String email, AccountType type) {
        Customer customer = findCustomer(email);
        if (accountRepository.existsByCustomerIdAndAccountType(customer.getId(), type)) {
            throw new DuplicateResourceException("You already have a " + type + " account");
        }
        return toResponse(createAccount(customer, type));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(String email) {
        Customer customer = findCustomer(email);
        return accountRepository.findByCustomerId(customer.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getMyAccount(String email, Long accountId) {
        Customer customer = findCustomer(email);
        return accountRepository.findByIdAndCustomerId(accountId, customer.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private Customer findCustomer(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private String generateAccountNumber() {
        String number;
        do {
            number = String.valueOf(secureRandom.nextLong(100_000_000_000L, 1_000_000_000_000L)); // 12 digits
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    private AccountResponse toResponse(Account a) {
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getAccountType(),
                a.getBalance(), a.getStatus(), a.getCreatedAt());
    }
}