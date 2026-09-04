package com.platform.staff.dto;

import java.util.List;
import java.util.UUID;

public record StaffDto(
        UUID id,
        UUID businessId,
        UUID userId,
        String displayName,
        String title,
        String bio,
        String imageUrl,
        String status,
        List<UUID> serviceIds
) {
}
