// CustomerController.java
package com.smartbank.controller;

import com.smartbank.dto.CustomerResponse;
import com.smartbank.service.CustomerService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) { this.customerService = customerService; }

    @GetMapping("/me")
    public CustomerResponse me(Authentication authentication) {
        return customerService.getProfile(authentication.getName());   // the email from the token
    }
}