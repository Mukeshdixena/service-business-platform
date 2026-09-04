package com.platform.resource.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateResourceRequest(
        @NotBlank(message = "must not be blank") String name,
        String type,
        String description,
        String imageUrl,
        String identifier
) {
}
