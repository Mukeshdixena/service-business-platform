package com.platform.business.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Management (owner/staff) view of a business — includes all fields. */
public record BusinessDto(
        UUID id,
        String name,
        String slug,
        String description,
        String phone,
        String email,
        String logoUrl,
        String coverImageUrl,
        String category,
        Set<String> capabilities,
        String status,
        String verificationStatus,
        Integer maxCapacity,
        Instant createdAt,
        Instant updatedAt
) {
}
