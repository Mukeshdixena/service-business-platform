package com.platform.membership.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePurchaseMembershipRequest(
        @NotNull(message = "must not be null") UUID membershipPlanId
) {
}
