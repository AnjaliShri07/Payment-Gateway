package org.paymentgateway.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.paymentgateway.auth.entity.JwtUser;
import org.paymentgateway.auth.entity.RefreshToken;
import org.paymentgateway.auth.exception.TokenExpiredException;
import org.paymentgateway.auth.exception.TokenInvalidException;
import org.paymentgateway.auth.exception.TokenRefreshException;
import org.paymentgateway.auth.repository.RefreshTokenRepository;
import org.paymentgateway.auth.repository.UserRepository;
import org.paymentgateway.auth.service.RefreshTokenService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTests {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(604800000L, refreshTokenRepository, userRepository);
    }

    @Test
    void createRefreshTokenCreatesAndSavesTokenForUser() {
        JwtUser user = new JwtUser();
        user.setId(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken token = service.createRefreshToken(10L);

        assertSame(user, token.getUser());
        assertNotNull(token.getToken());
        assertNotNull(token.getExpiryDate());
        assertFalse(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void verifyExpirationRejectsRevokedToken() {
        RefreshToken token = new RefreshToken();
        token.setToken("revoked");
        token.setRevoked(true);

        assertThrows(TokenRefreshException.class, () -> service.verifyExpiration(token));
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void verifyExpirationDeletesExpiredToken() {
        RefreshToken token = new RefreshToken();
        token.setToken("expired");
        token.setExpiryDate(Instant.now().minusSeconds(1));

        assertThrows(TokenExpiredException.class, () -> service.verifyExpiration(token));
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void verifyExpirationRejectsNullToken() {
        assertThrows(TokenInvalidException.class, () -> service.verifyExpiration(null));
    }

    @Test
    void revokeRefreshTokenMarksExistingTokenRevoked() {
        RefreshToken token = new RefreshToken();
        token.setToken("refresh");
        when(refreshTokenRepository.findByToken(" refresh ")).thenReturn(Optional.of(token));

        service.revokeRefreshToken(" refresh ");

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }
}
