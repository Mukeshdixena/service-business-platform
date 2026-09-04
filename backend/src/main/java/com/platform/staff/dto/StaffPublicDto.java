package com.platform.staff.dto;

import java.util.List;
import java.util.UUID;

/** Identical to {@link StaffDto} minus {@code userId} — never expose internal
 * user linkage publicly (CLAUDE_CODE.md §41). */
public record StaffPublicDto(
        UUID id,
        UUID businessId,
        String displayName,
        String title,
        String bio,
        String imageUrl,
        String status,
        List<UUID> serviceIds
) {
}
