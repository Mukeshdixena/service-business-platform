package com.platform.user.domain;

/**
 * Global platform-level role (API_CONTRACT.md: {@code PlatformRole}). Distinct
 * from {@code BusinessMembershipRole}, which is a per-business role.
 */
public enum PlatformRole {
    CUSTOMER,
    BUSINESS_OWNER,
    STAFF,
    ADMIN
}
