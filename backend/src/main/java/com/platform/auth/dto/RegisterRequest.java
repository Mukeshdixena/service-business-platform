package com.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "must not be blank") @Email(message = "must be a valid email") String email,
        @NotBlank(message = "must not be blank") @Size(min = 8, message = "must be at least 8 characters") String password,
        @NotBlank(message = "must not be blank") String fullName,
        @NotNull(message = "must not be null") String role
) {
}
