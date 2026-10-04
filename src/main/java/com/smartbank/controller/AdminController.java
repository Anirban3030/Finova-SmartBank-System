package com.smartbank.controller;

import com.smartbank.dto.AdminTransactionResponse;
import com.smartbank.dto.DashboardResponse;
import com.smartbank.dto.StatisticsResponse;
import com.smartbank.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) { this.adminService = adminService; }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() { return adminService.dashboard(); }

    @GetMapping("/statistics")
    public StatisticsResponse statistics() { return adminService.statistics(); }

    @GetMapping("/recent-transactions")
    public List<AdminTransactionResponse> recent(@RequestParam(defaultValue = "10") int limit) {
        return adminService.recentTransactions(limit);
    }
}