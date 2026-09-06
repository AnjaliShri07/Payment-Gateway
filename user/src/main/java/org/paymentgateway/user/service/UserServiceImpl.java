package org.paymentgateway.user.service;

import lombok.extern.slf4j.Slf4j;
import org.paymentgateway.user.DTO.UserUpdateRequest;
import org.paymentgateway.user.entity.User;
import org.paymentgateway.user.repository.UserRepository;
import org.springframework.stereotype.Service;

/**
 * Service implementation for user management workflows.
 * Provides CRUD behavior and user-specific queries for the payment gateway.
 */
@Slf4j
@Service
public class UserServiceImpl extends AbstractBaseService<User, Long> {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        super(userRepository);
        this.userRepository = userRepository;
    }

    @Override
    public User update(Long id, User user) {
        User existing = userRepository.findById(id).orElse(null);
        if (existing != null) {
            if (user.getUsername() != null) {
                existing.setUsername(user.getUsername());
            }
            if (user.getEmail() != null) {
                existing.setEmail(user.getEmail());
            }
            if (user.getPassword() != null) {
                existing.setPassword(user.getPassword());
            }
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                existing.setRoles(user.getRoles());
            }
            return userRepository.save(existing);
        }
        return null;
    }

    public User update(Long id, UserUpdateRequest request) {
        User existing = userRepository.findById(id).orElse(null);
        if (existing != null) {
            if (request.getUsername() != null) {
                existing.setUsername(request.getUsername());
            }
            if (request.getEmail() != null) {
                existing.setEmail(request.getEmail());
            }
            if (request.getPassword() != null) {
                existing.setPassword(request.getPassword());
            }
            if (request.getRoles() != null) {
                existing.setRoles(request.getRoles());
            }
            if (request.getEnabled() != null) {
                existing.setEnabled(request.getEnabled());
            }
            if (request.getAccountNonExpired() != null) {
                existing.setAccountNonExpired(request.getAccountNonExpired());
            }
            if (request.getCredentialsNonExpired() != null) {
                existing.setCredentialsNonExpired(request.getCredentialsNonExpired());
            }
            if (request.getAccountNonLocked() != null) {
                existing.setAccountNonLocked(request.getAccountNonLocked());
            }
            return userRepository.save(existing);
        }
        return null;
    }

    @Override
    public User findByEmail(String email) {
        return null;
    }
}
