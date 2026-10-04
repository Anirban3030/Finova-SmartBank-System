package com.smartbank.service;

import com.smartbank.dto.*;
import com.smartbank.entity.AccountType;
import com.smartbank.entity.Customer;
import com.smartbank.entity.Role;
import com.smartbank.exception.DuplicateResourceException;
import com.smartbank.repository.CustomerRepository;
import com.smartbank.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AccountService accountService;

    public AuthService(
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AccountService accountService) {

        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.accountService = accountService;
    }

    @Transactional
    public CustomerResponse register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase();

        // Check whether email already exists
        if (customerRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "Email is already registered"
            );
        }

        Customer customer = new Customer();

        customer.setName(request.name().trim());
        customer.setEmail(email);
        customer.setPhone(request.phone());

        // Hash password before storing it
        customer.setPassword(
                passwordEncoder.encode(request.password())
        );

        // Every newly registered user is CUSTOMER
        customer.setRole(Role.CUSTOMER);

        Customer saved = customerRepository.save(customer);

        // Create default savings account
        accountService.createAccount(
                saved,
                AccountType.SAVINGS
        );

        return new CustomerResponse(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getPhone(),
                saved.getRole()
        );
    }

    public AuthResponse login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase();

        // Spring Security checks:
        // 1. Does user exist?
        // 2. Is password correct?
        // 3. Is user enabled?
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.password()
                )
        );

        // Get user after successful authentication
        Customer customer = customerRepository
                .findByEmail(email)
                .orElseThrow();

        // Generate JWT
        String token = jwtService.generateToken(
                customer.getEmail(),
                customer.getRole()
        );

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds()
        );
    }
}