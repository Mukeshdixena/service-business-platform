package com.platform.business.dto;

import com.platform.business.domain.BusinessCapability;
import jakarta.validation.constraints.Email;

import java.util.Set;

/**
 * Partial update — all fields optional (null = leave unchanged). {@code slug}
 * and {@code category} are intentionally not editable after creation: the slug
 * is a public identifier third parties may already link to, and the category
 * describes what the business fundamentally is.
 */
public record UpdateBusinessRequest(
        String name,
        String description,
        String phone,
        @Email(message = "must be a valid email") String email,
        String logoUrl,
        String coverImageUrl,
        Set<BusinessCapability> capabilities
) {
}
