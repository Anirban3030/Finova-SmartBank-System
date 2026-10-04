package com.smartbank.controller;

import com.smartbank.dto.PageResponse;
import com.smartbank.dto.TransactionResponse;
import com.smartbank.entity.TransactionType;
import com.smartbank.service.TransactionHistoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionHistoryService historyService;

    public TransactionController(TransactionHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public PageResponse<TransactionResponse> history(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return historyService.search(authentication.getName(), accountId, type, from, to, page, size);
    }

    @GetMapping("/{id}")
    public TransactionResponse byId(@PathVariable Long id, Authentication authentication) {
        return historyService.getById(authentication.getName(), id);
    }
}