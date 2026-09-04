package com.platform.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Issues and validates short-lived JWT access tokens. Refresh tokens are
 * deliberately NOT JWTs (see {@code auth.service.AuthService}) — they are opaque,
 * server-tracked, revocable tokens stored hashed in the database, which is what
 * allows logout/rotation to actually invalidate them.
 */
@Component
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";

    private final SecretKey signingKey;
    private final long accessTokenTtlSeconds;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds) {
        // HS256 requires a key of at least 256 bits; dev/test secrets are padded via hashing
        // in production a real secret (>= 32 bytes) must be supplied.
        this.signingKey = Keys.hmacShaKeyFor(normalize(secret));
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    private static byte[] normalize(String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) {
            return raw;
        }
        // Never do this with a genuinely production secret (prod profile requires
        // JWT_SECRET to be set explicitly with no fallback) — this only protects
        // local/test convenience secrets from being too short for HS256.
        byte[] padded = new byte[32];
        for (int i = 0; i < 32; i++) {
            padded[i] = raw[i % raw.length];
        }
        return padded;
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public String generateAccessToken(UUID userId, String email, Set<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
                .signWith(signingKey)
                .compact();
    }

    public ParsedToken parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get(CLAIM_EMAIL, String.class);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get(CLAIM_ROLES, List.class);
            return new ParsedToken(userId, email, roles == null ? Set.of() : Set.copyOf(roles));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid or expired access token.");
        }
    }

    public record ParsedToken(UUID userId, String email, Set<String> roles) {
        public Set<String> authorities() {
            return roles.stream().map(r -> "ROLE_" + r).collect(Collectors.toUnmodifiableSet());
        }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
