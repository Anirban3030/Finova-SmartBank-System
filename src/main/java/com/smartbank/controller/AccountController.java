package com.smartbank.controller;

import com.smartbank.dto.*;
import com.smartbank.service.AccountService;
import com.smartbank.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<AccountResponse> myAccounts(Authentication authentication) {
        return accountService.getMyAccounts(authentication.getName());
    }

    @GetMapping("/{id}")
    public AccountResponse myAccount(@PathVariable Long id, Authentication authentication) {
        return accountService.getMyAccount(authentication.getName(), id);
    }

    @PostMapping
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody CreateAccountRequest request,
                                                Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.openAccount(authentication.getName(), request.accountType()));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<TransactionResponse> deposit(@PathVariable Long id,
                                                       @Valid @RequestBody  AmountRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.deposit(authentication.getName(), id, request.amount()));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@PathVariable Long id,
                                                        @Valid @RequestBody AmountRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.withdraw(authentication.getName(), id, request.amount()));
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<TransactionResponse> transfer(@PathVariable Long id,
                                                        @Valid @RequestBody TransferRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(authentication.getName(), id,
                        request.toAccountNumber(), request.amount()));
    }
}