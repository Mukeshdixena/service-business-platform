package com.platform.business.dto;

import com.platform.business.domain.BusinessCapability;
import com.platform.business.domain.BusinessCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record CreateBusinessRequest(
        @NotBlank(message = "must not be blank") String name,
        @Pattern(regexp = "^[a-z0-9-]+$", message = "must be lowercase letters, digits and hyphens only")
        String slug,
        String description,
        String phone,
        @Email(message = "must be a valid email") String email,
        String logoUrl,
        String coverImageUrl,
        @NotNull(message = "must not be null") BusinessCategory category,
        Set<BusinessCapability> capabilities,
        /** Only meaningful with the CAPACITY capability; must be > 0 when supplied. */
        @Positive(message = "must be > 0") Integer maxCapacity
) {
}
