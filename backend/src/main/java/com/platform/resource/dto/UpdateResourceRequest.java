package com.platform.resource.dto;

import com.platform.resource.domain.ResourceStatus;

/** Partial update — all fields optional; null means "leave unchanged". */
public record UpdateResourceRequest(
        String name,
        String type,
        String description,
        String imageUrl,
        String identifier,
        ResourceStatus status
) {
}
