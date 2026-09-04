package com.platform.staff.dto;

import com.platform.staff.domain.StaffStatus;

import java.util.List;
import java.util.UUID;

/** All fields optional — partial update; null means "leave unchanged" (serviceIds
 * null means "don't touch assignments", an empty list means "clear all"). */
public record UpdateStaffRequest(
        String displayName,
        String title,
        String bio,
        String imageUrl,
        StaffStatus status,
        List<UUID> serviceIds
) {
}
