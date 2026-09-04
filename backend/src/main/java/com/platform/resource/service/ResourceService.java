package com.platform.resource.service;

import com.platform.common.exception.NotFoundException;
import com.platform.common.pagination.PageResponse;
import com.platform.resource.domain.Resource;
import com.platform.resource.domain.ResourceStatus;
import com.platform.resource.dto.CreateResourceRequest;
import com.platform.resource.dto.ResourceDto;
import com.platform.resource.dto.UpdateResourceRequest;
import com.platform.resource.repository.ResourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Transactional
    public ResourceDto create(UUID businessId, CreateResourceRequest request) {
        Resource resource = new Resource(businessId, request.name(), request.type(), request.description(),
                request.imageUrl(), request.identifier());
        return toDto(resourceRepository.save(resource));
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceDto> list(UUID businessId, Pageable pageable) {
        Page<Resource> page = resourceRepository.findByBusinessId(businessId, pageable);
        return PageResponse.of(page, ResourceService::toDto);
    }

    /**
     * Public/discovery view (API_CONTRACT.md {@code BusinessPublicDto.resources}) —
     * everything except UNAVAILABLE, so a customer can see the fleet (including
     * items temporarily in MAINTENANCE) when picking one to rent. Callers gate on
     * the RESOURCES/RENTALS capability.
     */
    @Transactional(readOnly = true)
    public List<ResourceDto> listPublic(UUID businessId) {
        return resourceRepository.findByBusinessIdAndStatusNot(businessId, ResourceStatus.UNAVAILABLE).stream()
                .map(ResourceService::toDto)
                .toList();
    }

    @Transactional
    public ResourceDto update(UUID businessId, UUID resourceId, UpdateResourceRequest request) {
        Resource resource = getOwned(businessId, resourceId);
        resource.applyUpdate(request.name(), request.type(), request.description(), request.imageUrl(),
                request.identifier(), request.status());
        return toDto(resource);
    }

    /** Soft delete: sets UNAVAILABLE, since historical bookings still reference the row. */
    @Transactional
    public void softDelete(UUID businessId, UUID resourceId) {
        getOwned(businessId, resourceId).markUnavailable();
    }

    @Transactional(readOnly = true)
    public Resource getOwned(UUID businessId, UUID resourceId) {
        return resourceRepository.findByIdAndBusinessId(resourceId, businessId)
                .orElseThrow(() -> NotFoundException.of("Resource", resourceId));
    }

    public static ResourceDto toDto(Resource r) {
        return new ResourceDto(r.getId(), r.getBusinessId(), r.getName(), r.getType(), r.getDescription(),
                r.getImageUrl(), r.getIdentifier(), r.getStatus().name());
    }
}
