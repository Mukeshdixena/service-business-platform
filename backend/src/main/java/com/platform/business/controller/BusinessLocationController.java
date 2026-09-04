package com.platform.business.controller;

import com.platform.business.dto.CreateLocationRequest;
import com.platform.business.dto.LocationDto;
import com.platform.business.dto.UpdateLocationRequest;
import com.platform.business.service.BusinessLocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/locations")
public class BusinessLocationController {

    private final BusinessLocationService locationService;

    public BusinessLocationController(BusinessLocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public List<LocationDto> list(@PathVariable UUID businessId) {
        return locationService.list(businessId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public LocationDto create(@PathVariable UUID businessId, @Valid @RequestBody CreateLocationRequest request) {
        return locationService.create(businessId, request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public LocationDto update(@PathVariable UUID businessId, @PathVariable UUID id,
                               @Valid @RequestBody UpdateLocationRequest request) {
        return locationService.update(businessId, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID businessId, @PathVariable UUID id) {
        locationService.delete(businessId, id);
        return ResponseEntity.noContent().build();
    }
}
