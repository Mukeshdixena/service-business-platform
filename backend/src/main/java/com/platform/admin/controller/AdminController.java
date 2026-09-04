package com.platform.admin.controller;

import com.platform.admin.dto.AdminBusinessDto;
import com.platform.admin.dto.PlatformStatsDto;
import com.platform.admin.service.AdminService;
import com.platform.business.domain.VerificationStatus;
import com.platform.common.pagination.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public PlatformStatsDto getStats() {
        return adminService.getStats();
    }

    @GetMapping("/businesses")
    public PageResponse<AdminBusinessDto> listBusinesses(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.listBusinesses(status, page, size);
    }

    @PostMapping("/businesses/{id}/verify")
    public AdminBusinessDto verifyBusiness(
            @PathVariable UUID id,
            @RequestParam VerificationStatus status) {
        return adminService.verifyBusiness(id, status);
    }

    @PostMapping("/businesses/{id}/suspend")
    public AdminBusinessDto suspendBusiness(@PathVariable UUID id) {
        return adminService.suspendBusiness(id);
    }

    @PostMapping("/businesses/{id}/activate")
    public AdminBusinessDto activateBusiness(@PathVariable UUID id) {
        return adminService.activateBusiness(id);
    }
}
