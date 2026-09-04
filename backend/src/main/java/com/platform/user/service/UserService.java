package com.platform.user.service;

import com.platform.common.exception.NotFoundException;
import com.platform.user.domain.PlatformRole;
import com.platform.user.domain.User;
import com.platform.user.dto.UserDto;
import com.platform.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("User", id));
    }

    @Transactional(readOnly = true)
    public UserDto getById(UUID id) {
        return toDto(getEntityById(id));
    }

    public static UserDto toDto(User user) {
        Set<String> roles = user.getRoles().stream().map(PlatformRole::name).collect(Collectors.toSet());
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(), roles, user.getCreatedAt());
    }
}
