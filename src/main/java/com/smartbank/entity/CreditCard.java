package com.smartbank.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "credit_cards",
        indexes = @Index(name = "idx_credit_cards_customer", columnList = "customer_id"))
@Getter
@Setter
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "masked_card_number", nullable = false, length = 25)  // e.g. **** **** **** 1234
    private String maskedCardNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    private CardType cardType;

    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "available_credit", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableCredit;

    @Column(name = "outstanding_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount = BigDecimal.ZERO;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status = CardStatus.INACTIVE;
}