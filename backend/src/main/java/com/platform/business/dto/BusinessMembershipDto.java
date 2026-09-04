package com.platform.business.dto;

import java.util.UUID;

public record BusinessMembershipDto(
        UUID businessId,
        String businessName,
        String role
) {
}
