package com.platform.business.controller;

import com.platform.business.dto.BusinessDto;
import com.platform.business.dto.CreateBusinessRequest;
import com.platform.business.dto.UpdateBusinessRequest;
import com.platform.business.service.BusinessService;
import com.platform.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ROLE_BUSINESS_OWNER', 'ROLE_ADMIN')")
    public BusinessDto create(@Valid @RequestBody CreateBusinessRequest request) {
        return businessService.create(CurrentUser.id(), request);
    }

    @GetMapping("/{businessId}")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public BusinessDto get(@PathVariable UUID businessId) {
        return businessService.getById(businessId);
    }

    @PatchMapping("/{businessId}")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public BusinessDto update(@PathVariable UUID businessId, @Valid @RequestBody UpdateBusinessRequest request) {
        return businessService.update(businessId, request);
    }

    @PostMapping("/{businessId}/publish")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public BusinessDto publish(@PathVariable UUID businessId) {
        return businessService.publish(businessId);
    }
}
