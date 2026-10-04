package com.smartbank.repository;

import com.smartbank.entity.CardPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardPaymentRepository extends JpaRepository<CardPayment, Long> {
    List<CardPayment> findByCardIdOrderByPaymentDateDesc(Long cardId);
}