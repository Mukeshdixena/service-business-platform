package com.platform.auth.service;

import com.platform.auth.domain.RefreshToken;
import com.platform.auth.dto.AuthResponse;
import com.platform.auth.dto.LoginRequest;
import com.platform.auth.dto.RegisterRequest;
import com.platform.auth.repository.RefreshTokenRepository;
import com.platform.common.exception.UnauthenticatedException;
import com.platform.common.exception.ValidationException;
import com.platform.common.security.JwtService;
import com.platform.common.security.TokenHasher;
import com.platform.user.domain.PlatformRole;
import com.platform.user.domain.User;
import com.platform.user.repository.UserRepository;
import com.platform.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Set<PlatformRole> SELF_REGISTERABLE_ROLES =
            Set.of(PlatformRole.CUSTOMER, PlatformRole.BUSINESS_OWNER);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshTokenTtlSeconds;

    public AuthService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        @Value("${app.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        PlatformRole role = parseSelfRegisterableRole(request.role());
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ValidationException("email: already registered");
        }
        User user = new User(request.email().toLowerCase(), passwordEncoder.encode(request.password()),
                request.fullName(), Set.of(role));
        user = userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthenticatedException("Invalid email or password."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthenticatedException("Invalid email or password.");
        }
        return issueTokens(user);
    }

    /**
     * Rotates the refresh token on every use: the presented token is revoked and
     * a brand-new one issued, so a leaked-then-replayed old token is detectable
     * (it will already be revoked) and the blast radius of a stolen token is
     * limited to a single use.
     */
    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthenticatedException("Missing refresh token.");
        }
        String hash = TokenHasher.sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthenticatedException("Invalid refresh token."));
        if (!stored.isValid()) {
            throw new UnauthenticatedException("Refresh token expired or revoked.");
        }
        stored.revoke();
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new UnauthenticatedException("Invalid refresh token."));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String hash = TokenHasher.sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(RefreshToken::revoke);
    }

    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    private AuthResult issueTokens(User user) {
        Set<String> roleNames = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roleNames);
        String rawRefreshToken = TokenHasher.generateOpaqueToken();
        refreshTokenRepository.save(new RefreshToken(user.getId(), TokenHasher.sha256(rawRefreshToken),
                Instant.now().plusSeconds(refreshTokenTtlSeconds)));
        AuthResponse response = new AuthResponse(accessToken, "Bearer", jwtService.getAccessTokenTtlSeconds(),
                UserService.toDto(user));
        return new AuthResult(response, rawRefreshToken);
    }

    private PlatformRole parseSelfRegisterableRole(String rawRole) {
        try {
            PlatformRole role = PlatformRole.valueOf(rawRole);
            if (!SELF_REGISTERABLE_ROLES.contains(role)) {
                throw new ValidationException("role: must be one of " + SELF_REGISTERABLE_ROLES);
            }
            return role;
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("role: must be one of " + SELF_REGISTERABLE_ROLES);
        }
    }

    public record AuthResult(AuthResponse response, String rawRefreshToken) {
    }
}
