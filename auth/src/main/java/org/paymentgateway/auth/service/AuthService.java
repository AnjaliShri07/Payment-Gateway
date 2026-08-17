package org.paymentgateway.auth.service;

import jakarta.transaction.Transactional;
import org.paymentgateway.auth.dto.AuthResponse;
import org.paymentgateway.auth.entity.RefreshToken;
import org.paymentgateway.auth.entity.User;
import org.paymentgateway.auth.exception.ApiException;
import org.paymentgateway.auth.repository.RefreshTokenRepository;
import org.paymentgateway.auth.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * AuthService: handles registration, login, token refresh, logout.
 *
 * OpenAI docs: https://platform.openai.com/docs
 * Java docs: https://docs.oracle.com/en/java/
 */
@Service
public class AuthService extends BaseService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final long refreshTokenValiditySeconds = 1209600L; // 14 days

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ApiException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(
                user.getUsername(),
                Map.of("roles", user.getRole())
        );

        RefreshToken refreshToken = createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken.getToken(), jwtService.accessTokenValiditySeconds);
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new ApiException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new ApiException("Refresh token expired");
        }

        User user = refreshToken.getUser();
        String accessToken = jwtService.generateAccessToken(user.getUsername(), Map.of("roles", user.getRoles()));

        // rotate refresh token
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusSeconds(refreshTokenValiditySeconds));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, refreshToken.getToken(), jwtService.accessTokenValiditySeconds);
    }

    public void logout(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr)
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public User register(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new ApiException("Username already exists");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRoles("USER");
        return userRepository.save(user);
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusSeconds(refreshTokenValiditySeconds));
        return refreshTokenRepository.save(token);
    }
}