package com.platform.resource.controller;

import com.platform.common.pagination.PageResponse;
import com.platform.resource.dto.CreateResourceRequest;
import com.platform.resource.dto.ResourceDto;
import com.platform.resource.dto.UpdateResourceRequest;
import com.platform.resource.service.ResourceService;
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
@RequestMapping("/api/v1/businesses/{businessId}/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public PageResponse<ResourceDto> list(@PathVariable UUID businessId, Pageable pageable) {
        return resourceService.list(businessId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public ResourceDto create(@PathVariable UUID businessId, @Valid @RequestBody CreateResourceRequest request) {
        return resourceService.create(businessId, request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public ResourceDto update(@PathVariable UUID businessId, @PathVariable UUID id,
                               @Valid @RequestBody UpdateResourceRequest request) {
        return resourceService.update(businessId, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID businessId, @PathVariable UUID id) {
        resourceService.softDelete(businessId, id);
        return ResponseEntity.noContent().build();
    }
}
