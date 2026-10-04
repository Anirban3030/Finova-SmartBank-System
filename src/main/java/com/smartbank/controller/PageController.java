package com.smartbank.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the browser shell. Data continues to be supplied by the existing REST API.
 */
@Controller
public class PageController {

    @GetMapping({"/", "/login"})
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard() {
        return "customer/dashboard";
    }

    @GetMapping("/customer/accounts")
    public String accounts() {
        return "customer/accounts";
    }

    @GetMapping("/customer/transactions")
    public String transactions() {
        return "customer/transactions";
    }

    @GetMapping("/customer/beneficiaries")
    public String beneficiaries() {
        return "customer/beneficiaries";
    }

    @GetMapping("/customer/cards")
    public String cards() {
        return "customer/cards";
    }

    @GetMapping("/customer/ai")
    public String ai() {
        return "customer/ai";
    }

    @GetMapping("/customer/ai-assistant")
    public String aiAssistant() {
        return "customer/ai";
    }

    @GetMapping("/customer/profile")
    public String profile() {
        return "customer/profile";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }
}
