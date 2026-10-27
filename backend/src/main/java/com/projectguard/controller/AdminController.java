package com.projectguard.controller;

import com.projectguard.dto.admin.AdminStatsResponse;
import com.projectguard.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminController — secured by ROLE_ADMIN.
 * Provides platform-wide statistics and management endpoints for the admin dashboard.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * GET /api/admin/dashboard — simple health/identity check for admin.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<String> dashboard(Authentication authentication) {
        return ResponseEntity.ok("Admin dashboard accessed by: " + authentication.getName());
    }

    /**
     * GET /api/admin/stats — aggregate platform statistics.
     * Returns counts of users, students, projects, assessments, skills, etc.
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }
}