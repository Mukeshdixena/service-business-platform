package com.platform.admin.dto;

import java.util.UUID;
import java.util.List;

public record AdminUserDto(
    UUID id,
    String email,
    String fullName,
    List<String> roles,
    String createdAt
) {}
