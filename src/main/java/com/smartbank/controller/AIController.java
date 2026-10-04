package com.smartbank.controller;

import com.smartbank.dto.AskRequest;
import com.smartbank.dto.AskResponse;
import com.smartbank.service.AIService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) { this.aiService = aiService; }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request, Authentication authentication) {
        return new AskResponse(aiService.ask(authentication.getName(), request.question().trim()));
    }
}