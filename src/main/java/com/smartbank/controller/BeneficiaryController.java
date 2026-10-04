package com.smartbank.controller;

import com.smartbank.dto.BeneficiaryRequest;
import com.smartbank.dto.BeneficiaryResponse;
import com.smartbank.service.BeneficiaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> add(@Valid @RequestBody BeneficiaryRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(beneficiaryService.add(authentication.getName(), request));
    }

    @GetMapping
    public List<BeneficiaryResponse> list(Authentication authentication) {
        return beneficiaryService.list(authentication.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id, Authentication authentication) {
        beneficiaryService.remove(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}