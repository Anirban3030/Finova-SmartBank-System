// CustomerService.java
package com.smartbank.service;

import com.smartbank.dto.CustomerResponse;
import com.smartbank.entity.Customer;
import com.smartbank.repository.CustomerRepository;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponse getProfile(String email) {
        Customer c = customerRepository.findByEmail(email).orElseThrow();
        return new CustomerResponse(c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getRole());
    }
}