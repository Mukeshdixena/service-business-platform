package com.platform.auth.dto;

import com.platform.user.dto.UserDto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserDto user
) {
}
