package com.platform.staff.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record CreateStaffRequest(
        @NotBlank(message = "must not be blank") String displayName,
        String title,
        String bio,
        String imageUrl,
        List<UUID> serviceIds
) {
}
