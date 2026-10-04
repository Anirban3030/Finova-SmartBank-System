package com.smartbank.config;

import com.smartbank.entity.Customer;
import com.smartbank.entity.Role;
import com.smartbank.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(CustomerRepository customerRepository, PasswordEncoder passwordEncoder,
                       @Value("${app.admin.email:}") String adminEmail,
                       @Value("${app.admin.password:}") String adminPassword) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("Admin seeding skipped (ADMIN_EMAIL / ADMIN_PASSWORD not set)");
            return;
        }
        if (adminPassword.length() < 8) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least 8 characters");
        }

        String email = adminEmail.trim().toLowerCase();
        customerRepository.findByEmail(email).ifPresentOrElse(
                existing -> {
                    if (existing.getRole() == Role.ADMIN) {
                        log.info("Admin account already exists: {}", email);
                    } else {
                        log.warn("User {} already exists and is not an admin. Not modifying it.", email);
                    }
                },
                () -> {
                    Customer admin = new Customer();
                    admin.setName("Administrator");
                    admin.setEmail(email);
                    admin.setPhone("0000000000");
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setRole(Role.ADMIN);
                    customerRepository.save(admin);
                    log.info("Admin account created: {}", email);       // never log the password
                });
    }
}