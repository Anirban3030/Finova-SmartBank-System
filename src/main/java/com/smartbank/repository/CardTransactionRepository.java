package com.smartbank.repository;

import com.smartbank.entity.CardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardTransactionRepository extends JpaRepository<CardTransaction, Long> {
    List<CardTransaction> findByCardIdOrderByTransactionDateDesc(Long cardId);
}