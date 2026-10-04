package com.smartbank.controller;

import com.smartbank.dto.*;
import com.smartbank.service.CardService;
import com.smartbank.service.CardTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;
    private final CardTransactionService cardTransactionService;

    public CardController(CardService cardService, CardTransactionService cardTransactionService) {
        this.cardService = cardService;
        this.cardTransactionService = cardTransactionService;
    }

    @PostMapping("/request")
    public ResponseEntity<CardResponse> request(@Valid @RequestBody CardRequest request,
                                                Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardService.requestCard(authentication.getName(), request.cardType()));
    }

    @GetMapping
    public List<CardResponse> myCards(Authentication authentication) {
        return cardService.list(authentication.getName());
    }

    @GetMapping("/{id}")
    public CardResponse myCard(@PathVariable Long id, Authentication authentication) {
        return cardService.get(authentication.getName(), id);
    }

    @PutMapping("/{id}/activate")
    public CardResponse activate(@PathVariable Long id, Authentication authentication) {
        return cardService.activate(authentication.getName(), id);
    }

    @PutMapping("/{id}/block")
    public CardResponse block(@PathVariable Long id, Authentication authentication) {
        return cardService.block(authentication.getName(), id);
    }

    @PostMapping("/{id}/purchase")
    public ResponseEntity<CardTransactionResponse> purchase(@PathVariable Long id,
                                                            @Valid @RequestBody PurchaseRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardTransactionService.purchase(authentication.getName(), id,
                        request.merchant(), request.amount()));
    }

    @GetMapping("/{id}/transactions")
    public List<CardTransactionResponse> purchases(@PathVariable Long id, Authentication authentication) {
        return cardTransactionService.purchases(authentication.getName(), id);
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<CardPaymentResponse> pay(@PathVariable Long id,
                                                   @Valid @RequestBody CardPaymentRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardTransactionService.pay(authentication.getName(), id,
                        request.sourceAccountId(), request.amount()));
    }

    @GetMapping("/{id}/payments")
    public List<CardPaymentResponse> payments(@PathVariable Long id, Authentication authentication) {
        return cardTransactionService.payments(authentication.getName(), id);
    }
}