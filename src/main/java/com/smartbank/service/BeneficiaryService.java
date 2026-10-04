package com.smartbank.service;

import com.smartbank.dto.BeneficiaryRequest;
import com.smartbank.dto.BeneficiaryResponse;
import com.smartbank.entity.Beneficiary;
import com.smartbank.entity.BeneficiaryStatus;
import com.smartbank.entity.Customer;
import com.smartbank.exception.DuplicateResourceException;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.repository.BeneficiaryRepository;
import com.smartbank.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerRepository customerRepository;

    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository,
                              CustomerRepository customerRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public BeneficiaryResponse add(String email, BeneficiaryRequest request) {
        Customer customer = findCustomer(email);

        if (beneficiaryRepository.existsByCustomerIdAndAccountNumberAndStatus(
                customer.getId(), request.accountNumber(), BeneficiaryStatus.ACTIVE)) {
            throw new DuplicateResourceException("Beneficiary with this account number already exists");
        }

        Beneficiary b = new Beneficiary();
        b.setCustomer(customer);
        b.setName(request.name().trim());
        b.setAccountNumber(request.accountNumber());
        b.setBankName(request.bankName().trim());
        return toResponse(beneficiaryRepository.save(b));       // status defaults to ACTIVE
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> list(String email) {
        Customer customer = findCustomer(email);
        return beneficiaryRepository
                .findByCustomerIdAndStatusOrderByNameAsc(customer.getId(), BeneficiaryStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void remove(String email, Long id) {
        Customer customer = findCustomer(email);
        Beneficiary b = beneficiaryRepository
                .findByIdAndCustomerIdAndStatus(id, customer.getId(), BeneficiaryStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found"));
        b.setStatus(BeneficiaryStatus.INACTIVE);                // soft delete; dirty checking saves it
    }

    private Customer findCustomer(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private BeneficiaryResponse toResponse(Beneficiary b) {
        return new BeneficiaryResponse(b.getId(), b.getName(), b.getAccountNumber(),
                b.getBankName(), b.getStatus());
    }
}