package com.platform.catalog.controller;

import com.platform.catalog.dto.CreateServiceRequest;
import com.platform.catalog.dto.ServiceDto;
import com.platform.catalog.dto.UpdateServiceRequest;
import com.platform.catalog.service.CatalogService;
import com.platform.common.pagination.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/services")
public class ServiceController {

    private final CatalogService catalogService;

    public ServiceController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public PageResponse<ServiceDto> list(@PathVariable UUID businessId, Pageable pageable) {
        return catalogService.list(businessId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public ServiceDto create(@PathVariable UUID businessId, @Valid @RequestBody CreateServiceRequest request) {
        return catalogService.create(businessId, request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public ServiceDto update(@PathVariable UUID businessId, @PathVariable UUID id,
                              @Valid @RequestBody UpdateServiceRequest request) {
        return catalogService.update(businessId, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID businessId, @PathVariable UUID id) {
        catalogService.softDelete(businessId, id);
        return ResponseEntity.noContent().build();
    }
}
