package com.platform.business.controller;

import com.platform.business.dto.BusinessHoursDto;
import com.platform.business.dto.UpsertBusinessHoursRequest;
import com.platform.business.service.BusinessHoursService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/hours")
public class BusinessHoursController {

    private final BusinessHoursService hoursService;

    public BusinessHoursController(BusinessHoursService hoursService) {
        this.hoursService = hoursService;
    }

    @GetMapping
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public List<BusinessHoursDto> list(@PathVariable UUID businessId) {
        return hoursService.list(businessId);
    }

    @PutMapping
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public List<BusinessHoursDto> replace(@PathVariable UUID businessId,
                                           @Valid @RequestBody UpsertBusinessHoursRequest request) {
        return hoursService.replace(businessId, request);
    }
}
