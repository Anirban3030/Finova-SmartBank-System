package com.smartbank.service;

import com.smartbank.dto.CardResponse;
import com.smartbank.entity.CardStatus;
import com.smartbank.entity.CardType;
import com.smartbank.entity.CreditCard;
import com.smartbank.entity.Customer;
import com.smartbank.exception.BusinessRuleException;
import com.smartbank.exception.DuplicateResourceException;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.repository.CreditCardRepository;
import com.smartbank.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.YearMonth;
import java.util.List;

@Service
public class CardService {

    private static final List<CardStatus> USABLE = List.of(CardStatus.INACTIVE, CardStatus.ACTIVE);

    private final CreditCardRepository cardRepository;
    private final CustomerRepository customerRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public CardService(CreditCardRepository cardRepository, CustomerRepository customerRepository) {
        this.cardRepository = cardRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CardResponse requestCard(String email, CardType type) {
        Customer customer = findCustomer(email);

        if (cardRepository.existsByCustomerIdAndCardTypeAndStatusIn(customer.getId(), type, USABLE)) {
            throw new DuplicateResourceException("You already hold a " + type + " card");
        }

        CreditCard card = new CreditCard();
        card.setCustomer(customer);
        card.setCardType(type);
        card.setMaskedCardNumber("**** **** **** " + String.format("%04d", secureRandom.nextInt(10_000)));
        card.setCreditLimit(type.getDefaultLimit());
        card.setAvailableCredit(type.getDefaultLimit());
        card.setOutstandingAmount(BigDecimal.ZERO.setScale(2));
        card.setExpiryDate(YearMonth.now().plusYears(5).atEndOfMonth());
        card.setStatus(CardStatus.INACTIVE);

        return toResponse(cardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public List<CardResponse> list(String email) {
        Customer customer = findCustomer(email);
        return cardRepository.findByCustomerId(customer.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CardResponse get(String email, Long cardId) {
        Customer customer = findCustomer(email);
        return cardRepository.findByIdAndCustomerId(cardId, customer.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    }

    @Transactional
    public CardResponse activate(String email, Long cardId) {
        CreditCard card = lockOwnedCard(email, cardId);
        if (card.getStatus() != CardStatus.INACTIVE) {
            throw new BusinessRuleException("Only an inactive card can be activated");
        }
        card.setStatus(CardStatus.ACTIVE);
        return toResponse(card);
    }

    @Transactional
    public CardResponse block(String email, Long cardId) {
        CreditCard card = lockOwnedCard(email, cardId);
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BusinessRuleException("Card is already blocked");
        }
        if (card.getStatus() != CardStatus.INACTIVE && card.getStatus() != CardStatus.ACTIVE) {
            throw new BusinessRuleException("This card cannot be blocked");
        }
        card.setStatus(CardStatus.BLOCKED);
        return toResponse(card);
    }

    private CreditCard lockOwnedCard(String email, Long cardId) {
        Customer customer = findCustomer(email);
        return cardRepository.findByIdAndCustomerIdForUpdate(cardId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    }

    private Customer findCustomer(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private CardResponse toResponse(CreditCard c) {
        return new CardResponse(c.getId(), c.getMaskedCardNumber(), c.getCardType(),
                c.getCreditLimit(), c.getAvailableCredit(), c.getOutstandingAmount(),
                c.getExpiryDate(), c.getStatus());
    }
}