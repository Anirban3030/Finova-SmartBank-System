package com.smartbank.repository;

import com.smartbank.entity.Customer;
import java.util.Optional;

import com.smartbank.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);   // login
    boolean existsByEmail(String email);
    long countByRole(Role role);// duplicate check at registration
}