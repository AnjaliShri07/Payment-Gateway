package org.paymentgateway.auth.service;

import org.paymentgateway.auth.dto.response.UserProfileResponse;
import org.paymentgateway.auth.entity.JwtUser;
import org.paymentgateway.auth.exception.BadRequestException;
import org.paymentgateway.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserProfileResponse> getUserProfile(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }

        return userRepository.findById(userId)
            .map(this::mapToUserProfileResponse);
    }

    @Transactional(readOnly = true)
    public Optional<UserProfileResponse> getUserProfileByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BadRequestException("Username cannot be blank");
        }

        return userRepository.findByUsername(username.trim())
            .map(this::mapToUserProfileResponse);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .filter(Objects::nonNull)
            .map(this::mapToUserProfileResponse)
            .collect(Collectors.toList());
    }

    private UserProfileResponse mapToUserProfileResponse(JwtUser user) {
        List<String> roles = user.getRoles() != null
            ? user.getRoles().stream()
                .filter(Objects::nonNull)
                .filter(role -> role.getName() != null)
                .map(role -> role.getName().name())
                .collect(Collectors.toList())
            : Collections.emptyList();

        return new UserProfileResponse(
            user.getId(),
            Objects.requireNonNullElse(user.getUsername(), ""),
            Objects.requireNonNullElse(user.getEmail(), ""),
            roles,
            user.isEnabled(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public Optional<JwtUser> loadUserByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BadRequestException("Username cannot be blank");
        }

        return userRepository.findByUsername(username.trim());
    }
}
