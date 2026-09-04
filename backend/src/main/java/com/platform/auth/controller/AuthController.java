package com.platform.auth.controller;

import com.platform.auth.dto.AuthResponse;
import com.platform.auth.dto.LoginRequest;
import com.platform.auth.dto.RegisterRequest;
import com.platform.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final String cookieName;
    private final String cookiePath;

    public AuthController(AuthService authService,
                           @Value("${app.jwt.refresh-cookie-name}") String cookieName,
                           @Value("${app.jwt.refresh-cookie-path}") String cookiePath) {
        this.authService = authService;
        this.cookieName = cookieName;
        this.cookiePath = cookiePath;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthService.AuthResult result = authService.register(request);
        setRefreshCookie(response, result.rawRefreshToken(), authService.getRefreshTokenTtlSeconds());
        return result.response();
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.AuthResult result = authService.login(request);
        setRefreshCookie(response, result.rawRefreshToken(), authService.getRefreshTokenTtlSeconds());
        return result.response();
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(name = "${app.jwt.refresh-cookie-name}", required = false) String refreshToken,
                                 HttpServletResponse response) {
        AuthService.AuthResult result = authService.refresh(refreshToken);
        setRefreshCookie(response, result.rawRefreshToken(), authService.getRefreshTokenTtlSeconds());
        return result.response();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name = "${app.jwt.refresh-cookie-name}", required = false) String refreshToken,
                        HttpServletResponse response) {
        authService.logout(refreshToken);
        clearRefreshCookie(response);
    }

    private void setRefreshCookie(HttpServletResponse response, String rawToken, long ttlSeconds) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, rawToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(cookiePath)
                .maxAge(ttlSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(cookiePath)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
