package com.platform.business.service;

import com.platform.business.domain.BusinessMembershipRole;
import com.platform.business.repository.BusinessMembershipRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The single, reusable place where "is this caller allowed to touch this
 * business's data" is decided (CLAUDE_CODE.md §31). Every
 * {@code /businesses/{businessId}/**} management endpoint calls into this via
 * {@code @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")}
 * instead of hand-rolling the check — the businessId path variable is NEVER
 * trusted on its own.
 */
@Service("businessAccessService")
public class BusinessAccessService {

    private final BusinessMembershipRepository membershipRepository;

    public BusinessAccessService(BusinessMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    /**
     * @param requiredRole {@code "OWNER"} requires an OWNER membership exactly;
     *                      {@code "STAFF"} accepts either STAFF or OWNER (i.e. any
     *                      membership at all is "at least staff-level" access).
     *                      Platform ADMIN always passes, regardless of membership.
     */
    public boolean hasRole(UUID businessId, Authentication authentication, String requiredRole) {
        if (authentication == null || businessId == null) {
            return false;
        }
        if (isAdmin(authentication)) {
            return true;
        }
        UUID userId = currentUserId(authentication);
        if (userId == null) {
            return false;
        }
        return membershipRepository.findByBusinessIdAndUserId(businessId, userId)
                .map(membership -> satisfies(membership.getRole(), requiredRole))
                .orElse(false);
    }

    private boolean satisfies(BusinessMembershipRole actual, String required) {
        if ("OWNER".equals(required)) {
            return actual == BusinessMembershipRole.OWNER;
        }
        // STAFF-level requirement: OWNER or STAFF both qualify.
        return actual == BusinessMembershipRole.OWNER || actual == BusinessMembershipRole.STAFF;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof com.platform.common.security.AuthenticatedUser user) {
            return user.userId();
        }
        return null;
    }
}
