package com.platform.business.domain;

/**
 * This phase only implements the DRAFT -> ACTIVE transition (via the publish
 * endpoint). SUSPENDED/ARCHIVED are reserved for the future admin/moderation
 * phase but are defined now so the enum set is stable for the frontend.
 */
public enum BusinessStatus {
    DRAFT,
    ACTIVE,
    SUSPENDED,
    ARCHIVED
}
