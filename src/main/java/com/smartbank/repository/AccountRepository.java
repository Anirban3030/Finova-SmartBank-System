package com.smartbank.repository;

import com.smartbank.entity.Account;
import com.smartbank.entity.AccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByCustomerId(Long customerId);
    Optional<Account> findByIdAndCustomerId(Long id, Long customerId);  // ownership check
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);                // unique number generation
    boolean existsByCustomerIdAndAccountType(Long customerId, AccountType accountType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id and a.customer.id = :customerId")
    Optional<Account> findByIdAndCustomerIdForUpdate(@Param("id") Long id,
                                                     @Param("customerId") Long customerId);

    boolean existsByIdAndCustomerId(Long id, Long customerId);

    @Query("select a.id from Account a where a.accountNumber = :accountNumber")
    Optional<Long> findIdByAccountNumber(@Param("accountNumber") String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    @Query("select sum(a.balance) from Account a")
    BigDecimal sumBalances();

    @Query("select a.accountType, count(a) from Account a group by a.accountType")
    List<Object[]> countGroupedByType();
}