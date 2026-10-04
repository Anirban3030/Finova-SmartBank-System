package com.smartbank.repository;

import com.smartbank.entity.Beneficiary;
import com.smartbank.entity.BeneficiaryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    List<Beneficiary> findByCustomerId(Long customerId);

    boolean existsByCustomerIdAndAccountNumberAndStatus(Long customerId, String accountNumber,
                                                        BeneficiaryStatus status);

    List<Beneficiary> findByCustomerIdAndStatusOrderByNameAsc(Long customerId, BeneficiaryStatus status);

    Optional<Beneficiary> findByIdAndCustomerIdAndStatus(Long id, Long customerId, BeneficiaryStatus status);
}