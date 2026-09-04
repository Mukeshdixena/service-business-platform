package com.platform.availability.controller;

import com.platform.availability.dto.AvailabilityResponse;
import com.platform.availability.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Deliberately NOT gated by business membership: any authenticated user (i.e.
 * a prospective customer) must be able to check availability at any business
 * before booking, not just businesses they belong to. Contrast with the
 * sibling controllers under /businesses/{businessId}/** that ARE
 * membership-gated management endpoints.
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public AvailabilityResponse getAvailability(@PathVariable UUID businessId,
                                                 @RequestParam UUID serviceId,
                                                 @RequestParam(required = false) UUID staffId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return availabilityService.getAvailability(businessId, serviceId, staffId, date);
    }
}
