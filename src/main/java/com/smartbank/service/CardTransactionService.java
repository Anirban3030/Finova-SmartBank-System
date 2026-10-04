package com.smartbank.service;

import com.smartbank.dto.CardPaymentResponse;
import com.smartbank.dto.CardTransactionResponse;
import com.smartbank.entity.*;
import com.smartbank.exception.BusinessRuleException;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CardTransactionService {

    private final CreditCardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final CardPaymentRepository cardPaymentRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;

    public CardTransactionService(CreditCardRepository cardRepository,
                                  AccountRepository accountRepository,
                                  CardTransactionRepository cardTransactionRepository,
                                  CardPaymentRepository cardPaymentRepository,
                                  TransactionRepository transactionRepository,
                                  CustomerRepository customerRepository) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.cardTransactionRepository = cardTransactionRepository;
        this.cardPaymentRepository = cardPaymentRepository;
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CardTransactionResponse purchase(String email, Long cardId, String merchant, BigDecimal amount) {
        Customer customer = findCustomer(email);
        CreditCard card = lockOwnedCard(customer, cardId);
        requireUsable(card);

        BigDecimal value = amount.setScale(2);
        if (value.compareTo(card.getAvailableCredit()) > 0) {
            throw new BusinessRuleException("Insufficient credit limit");
        }

        card.setAvailableCredit(card.getAvailableCredit().subtract(value));
        card.setOutstandingAmount(card.getOutstandingAmount().add(value));

        CardTransaction purchase = new CardTransaction();
        purchase.setCard(card);
        purchase.setMerchant(merchant.trim());
        purchase.setAmount(value);                       // status defaults to SUCCESS
        return toResponse(cardTransactionRepository.save(purchase));
    }

    @Transactional
    public CardPaymentResponse pay(String email, Long cardId, Long sourceAccountId, BigDecimal amount) {
        Customer customer = findCustomer(email);
        CreditCard card = lockOwnedCard(customer, cardId);          // lock order: card first ...
        BigDecimal value = amount.setScale(2);

        if (value.compareTo(card.getOutstandingAmount()) > 0) {
            throw new BusinessRuleException("Payment exceeds the outstanding amount");
        }

        Account account = accountRepository                          // ... then the account
                .findByIdAndCustomerIdForUpdate(sourceAccountId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Account is not active");
        }
        if (account.getBalance().compareTo(value) < 0) {
            throw new BusinessRuleException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(value));
        card.setOutstandingAmount(card.getOutstandingAmount().subtract(value));
        card.setAvailableCredit(card.getAvailableCredit().add(value));

        CardPayment payment = new CardPayment();
        payment.setCard(card);
        payment.setAmount(value);                                    // status defaults to SUCCESS
        payment = cardPaymentRepository.save(payment);

        Transaction debit = new Transaction();
        debit.setAccount(account);
        debit.setTransactionType(TransactionType.CARD_PAYMENT);
        debit.setAmount(value);
        debit.setReference("CARDPAY-" + payment.getId());
        transactionRepository.save(debit);

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<CardTransactionResponse> purchases(String email, Long cardId) {
        requireOwnedCard(email, cardId);
        return cardTransactionRepository.findByCardIdOrderByTransactionDateDesc(cardId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CardPaymentResponse> payments(String email, Long cardId) {
        requireOwnedCard(email, cardId);
        return cardPaymentRepository.findByCardIdOrderByPaymentDateDesc(cardId)
                .stream().map(this::toResponse).toList();
    }

    // ---------- helpers ----------

    private void requireUsable(CreditCard card) {
        switch (card.getStatus()) {
            case ACTIVE -> { }
            case INACTIVE -> throw new BusinessRuleException("Card is not activated");
            case BLOCKED -> throw new BusinessRuleException("Card is blocked");
            default -> throw new BusinessRuleException("Card is not usable");
        }
        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Card has expired");
        }
    }

    private CreditCard lockOwnedCard(Customer customer, Long cardId) {
        return cardRepository.findByIdAndCustomerIdForUpdate(cardId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    }

    private void requireOwnedCard(String email, Long cardId) {
        Customer customer = findCustomer(email);
        cardRepository.findByIdAndCustomerId(cardId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    }

    private Customer findCustomer(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private CardTransactionResponse toResponse(CardTransaction t) {
        return new CardTransactionResponse(t.getId(), t.getCard().getId(), t.getMerchant(),
                t.getAmount(), t.getStatus(), t.getTransactionDate());
    }

    private CardPaymentResponse toResponse(CardPayment p) {
        return new CardPaymentResponse(p.getId(), p.getCard().getId(), p.getAmount(),
                p.getStatus(), p.getPaymentDate());
    }
}