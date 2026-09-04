package com.platform.attendance.controller;

import com.platform.attendance.dto.CapacityResponse;
import com.platform.attendance.service.AttendanceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Live occupancy view. OWNER/STAFF only this phase — a public "how busy is the
 * gym right now" display is a Phase 8+ discovery concern (API_CONTRACT.md).
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/capacity")
public class CapacityController {

    private final AttendanceService attendanceService;

    public CapacityController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public CapacityResponse capacity(@PathVariable UUID businessId) {
        return attendanceService.capacity(businessId);
    }
}
