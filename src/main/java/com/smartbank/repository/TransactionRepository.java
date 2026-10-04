package com.smartbank.repository;

import com.smartbank.entity.Transaction;
import com.smartbank.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountIdOrderByTransactionDateDesc(Long accountId);

    @Query("""
        select t from Transaction t
        where t.account.id in :accountIds
          and t.transactionType in :types
          and t.transactionDate >= :start
          and t.transactionDate < :end
        order by t.transactionDate desc, t.id desc
        """)
    Page<Transaction> search(@Param("accountIds") List<Long> accountIds,
                             @Param("types") List<TransactionType> types,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end,
                             Pageable pageable);

    @Query("select t from Transaction t where t.id = :id and t.account.customer.id = :customerId")
    Optional<Transaction> findByIdAndCustomerId(@Param("id") Long id, @Param("customerId") Long customerId);

    @Query("select count(t) from Transaction t where t.transactionDate >= :start and t.transactionDate < :end")
    long countBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select t.transactionType, count(t), sum(t.amount) from Transaction t group by t.transactionType")
    List<Object[]> summaryByType();

    @Query(value = """
        select cast(transaction_date as date) as tx_date, count(*) as cnt, sum(amount) as total
        from transactions
        where transaction_date >= :start
        group by cast(transaction_date as date)
        order by tx_date
        """, nativeQuery = true)
    List<Object[]> dailySummarySince(@Param("start") LocalDateTime start);

    @Query("select t from Transaction t join fetch t.account order by t.transactionDate desc, t.id desc")
    List<Transaction> findRecent(Pageable pageable);
}