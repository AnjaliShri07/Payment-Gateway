package org.paymentgateway.auth.service;

import org.paymentgateway.auth.dto.response.UserProfileResponse;
import org.paymentgateway.auth.entity.User;
import org.paymentgateway.auth.exception.BadRequestException;
import org.paymentgateway.auth.exception.UserNotFoundException;
import org.paymentgateway.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> UserNotFoundException.withId(userId));
        return mapToUserProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BadRequestException("Username cannot be blank");
        }

        User user = userRepository.findByUsername(username.trim())
            .orElseThrow(() -> UserNotFoundException.withIdentifier(username));
        return mapToUserProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .filter(Objects::nonNull)
            .map(this::mapToUserProfileResponse)
            .collect(Collectors.toList());
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        if (user == null) {
            return null;
        }

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
}
