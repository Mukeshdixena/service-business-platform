package com.platform.resource.dto;

import java.util.UUID;

public record ResourceDto(
        UUID id,
        UUID businessId,
        String name,
        String type,
        String description,
        String imageUrl,
        String identifier,
        String status
) {
}
