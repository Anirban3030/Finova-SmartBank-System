package com.smartbank.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "beneficiaries",
        indexes = @Index(name = "idx_beneficiaries_customer", columnList = "customer_id"))
@Getter
@Setter
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BeneficiaryStatus status = BeneficiaryStatus.ACTIVE;
}