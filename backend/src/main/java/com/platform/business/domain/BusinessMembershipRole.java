package com.platform.business.domain;

/**
 * Per-business role (API_CONTRACT.md: {@code BusinessMembershipRole}), distinct
 * from the global {@code PlatformRole}. A single user can hold different roles
 * on different businesses.
 */
public enum BusinessMembershipRole {
    OWNER,
    STAFF
}
