package com.platform.common.security;

import java.util.UUID;

/**
 * The principal attached to {@code Authentication} once a JWT access token has
 * been validated. Deliberately minimal — role/authority information travels via
 * {@code Authentication#getAuthorities()}, not duplicated here.
 */
public record AuthenticatedUser(UUID userId, String email) {
}
