package com.smartbank.security;

import com.smartbank.entity.Customer;
import com.smartbank.entity.CustomerStatus;
import com.smartbank.repository.CustomerRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public CustomUserDetailsService(
            CustomerRepository customerRepository) {

        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Customer customer = customerRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"
                        )
                );

        return User
                .withUsername(customer.getEmail())
                .password(customer.getPassword())

                // Converts CUSTOMER → ROLE_CUSTOMER
                // Converts ADMIN → ROLE_ADMIN
                .roles(customer.getRole().name())

                // Blocked users cannot authenticate
                .disabled(
                        customer.getStatus() ==
                                CustomerStatus.BLOCKED
                )

                .build();
    }
}