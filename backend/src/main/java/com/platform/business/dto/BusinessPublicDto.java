package com.platform.business.dto;

import com.platform.catalog.dto.ServicePublicDto;
import com.platform.membership.dto.MembershipPlanDto;
import com.platform.staff.dto.StaffPublicDto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Public discovery view — never exposes {@code status}, {@code verificationStatus}
 * or anything internal (per CLAUDE_CODE.md §41). The list endpoint omits
 * services/staff/hours by leaving them {@code null}, which Jackson then drops
 * from the JSON entirely (see {@code spring.jackson.default-property-inclusion}).
 *
 * <p>{@code membershipPlans} is always a (possibly empty) list, never null —
 * populated with only {@code ACTIVE} plans, and only when the business has the
 * MEMBERSHIPS capability, in both the summary list and full profile views.
 * It's the only way a customer browses plans before purchasing, since
 * {@code GET /businesses/{businessId}/membership-plans} is OWNER/STAFF-only.
 */
public record BusinessPublicDto(
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
        List<LocationDto> locations,
        List<ServicePublicDto> services,
        List<StaffPublicDto> staff,
        List<BusinessHoursDto> hours,
        List<MembershipPlanDto> membershipPlans
) {
}
