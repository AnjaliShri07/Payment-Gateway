package org.paymentgateway.auth.service;

import org.paymentgateway.auth.entity.RefreshToken;
import org.paymentgateway.auth.entity.JwtUser;
import org.paymentgateway.auth.exception.TokenExpiredException;
import org.paymentgateway.auth.exception.TokenInvalidException;
import org.paymentgateway.auth.exception.TokenRefreshException;
import org.paymentgateway.auth.exception.UserNotFoundException;
import org.paymentgateway.auth.repository.RefreshTokenRepository;
import org.paymentgateway.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final long refreshTokenDurationMs;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(
        @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenDurationMs,
        RefreshTokenRepository refreshTokenRepository,
        UserRepository userRepository
    ) {
        this.refreshTokenDurationMs = refreshTokenDurationMs > 0 ? refreshTokenDurationMs : 604800000L;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public Optional<RefreshToken> findByToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        JwtUser user = userRepository.findById(userId)
            .orElseThrow(() -> UserNotFoundException.withId(userId));

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
            .orElseGet(() -> {
                RefreshToken newToken = new RefreshToken();
                newToken.setUser(user);
                return newToken;
            });

        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString());
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token == null) {
            throw new TokenInvalidException("Refresh token is null or missing");
        }

        if (token.isRevoked()) {
            throw new TokenRefreshException(token.getToken(), "Refresh token has been revoked. Please sign in again.");
        }

        if (token.getExpiryDate() == null || token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenExpiredException(token.getToken(), "Refresh token has expired. Please sign in again.");
        }

        return token;
    }

    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken existingToken) {
        if (existingToken == null) {
            throw new TokenInvalidException("Existing refresh token cannot be null for rotation");
        }

        verifyExpiration(existingToken);

        existingToken.setToken(UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString());
        existingToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        existingToken.setRevoked(false);

        return refreshTokenRepository.save(existingToken);
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        if (StringUtils.hasText(token)) {
            refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
                refreshToken.setRevoked(true);
                refreshTokenRepository.save(refreshToken);
            });
        }
    }

    @Transactional
    public int deleteByUserId(Long userId) {
        if (userId == null) {
            return 0;
        }
        return userRepository.findById(userId)
            .map(refreshTokenRepository::deleteByUser)
            .orElse(0);
    }
}
