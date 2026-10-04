package com.smartbank.repository;

import com.smartbank.entity.CardStatus;
import com.smartbank.entity.CardType;
import com.smartbank.entity.CreditCard;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {
    List<CreditCard> findByCustomerId(Long customerId);
    Optional<CreditCard> findByIdAndCustomerId(Long id, Long customerId);
    boolean existsByCustomerIdAndCardTypeAndStatusIn(Long customerId, CardType cardType,
                                                     Collection<CardStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CreditCard c where c.id = :id and c.customer.id = :customerId")
    Optional<CreditCard> findByIdAndCustomerIdForUpdate(@Param("id") Long id,
                                                        @Param("customerId") Long customerId);

    @Query("select sum(c.outstandingAmount) from CreditCard c")
    BigDecimal sumOutstanding();

    @Query("select c.status, count(c) from CreditCard c group by c.status")
    List<Object[]> countGroupedByStatus();
}