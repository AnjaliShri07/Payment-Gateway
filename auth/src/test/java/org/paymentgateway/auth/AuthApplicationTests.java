package org.paymentgateway.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.paymentgateway.auth.constants.ERole;
import org.paymentgateway.auth.dto.request.LoginRequest;
import org.paymentgateway.auth.dto.request.LogoutRequest;
import org.paymentgateway.auth.dto.request.RegisterRequest;
import org.paymentgateway.auth.dto.request.TokenRefreshRequest;
import org.paymentgateway.auth.dto.response.AuthResponse;
import org.paymentgateway.auth.dto.response.TokenRefreshResponse;
import org.paymentgateway.auth.entity.JwtUser;
import org.paymentgateway.auth.entity.RefreshToken;
import org.paymentgateway.auth.entity.Role;
import org.paymentgateway.auth.exception.BadRequestException;
import org.paymentgateway.auth.exception.InvalidRoleException;
import org.paymentgateway.auth.exception.UserAlreadyExistsException;
import org.paymentgateway.auth.repository.RoleRepository;
import org.paymentgateway.auth.repository.UserRepository;
import org.paymentgateway.auth.security.JwtUserDetails;
import org.paymentgateway.auth.security.jwt.JwtTokenProvider;
import org.paymentgateway.auth.service.AuthenticationService;
import org.paymentgateway.auth.service.RefreshTokenService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthApplicationTests {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
            authenticationManager,
            userRepository,
            roleRepository,
            passwordEncoder,
            jwtTokenProvider,
            refreshTokenService
        );
    }

    @Test
    void registerCreatesUserWithDefaultUserRole() {
        Role userRole = new Role(ERole.ROLE_USER);
        when(userRepository.existsByUsername(" alice ")).thenReturn(false);
        when(userRepository.existsByEmail("ALICE@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("hashed-password");
        when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));

        authenticationService.register(new RegisterRequest(
            " alice ",
            "ALICE@example.com",
            "Password1!",
            null
        ));

        ArgumentCaptor<JwtUser> captor = ArgumentCaptor.forClass(JwtUser.class);
        verify(userRepository).save(captor.capture());
        JwtUser savedUser = captor.getValue();

        assertEquals("alice", savedUser.getUsername());
        assertEquals("alice@example.com", savedUser.getEmail());
        assertEquals("hashed-password", savedUser.getPassword());
        assertEquals(Set.of(userRole), savedUser.getRoles());
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(
            UserAlreadyExistsException.class,
            () -> authenticationService.register(
                new RegisterRequest("alice", "alice@example.com", "Password1!", null)
            )
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsUnknownRole() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);

        assertThrows(
            InvalidRoleException.class,
            () -> authenticationService.register(
                new RegisterRequest("alice", "alice@example.com", "Password1!", Set.of("OWNER"))
            )
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsNullRequest() {
        assertThrows(BadRequestException.class, () -> authenticationService.register(null));
        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void loginAuthenticatesUserCreatesTokensAndReturnsProfile() {
        JwtUserDetails details = new JwtUserDetails(
            7L,
            "alice",
            "alice@example.com",
            "hashed-password",
            java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")),
            true,
            true,
            true,
            true
        );
        Authentication authentication = mock(Authentication.class);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(details);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("access-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(7L)).thenReturn(refreshToken);

        AuthResponse response = authenticationService.login(
            new LoginRequest(" alice ", "Password1!")
        );

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());
        assertEquals(7L, response.id());
        assertEquals("alice", response.username());
        assertEquals("alice@example.com", response.email());
        assertEquals(java.util.List.of("ROLE_USER"), response.roles());
        verify(refreshTokenService).createRefreshToken(7L);
    }

    @Test
    void refreshTokenRotatesTokenAndIssuesNewAccessToken() {
        JwtUser user = new JwtUser();
        user.setId(7L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        RefreshToken existing = new RefreshToken();
        existing.setToken("rotated-refresh");
        existing.setUser(user);

        when(refreshTokenService.findByToken("old-refresh")).thenReturn(Optional.of(existing));
        when(refreshTokenService.verifyExpiration(existing)).thenReturn(existing);
        when(refreshTokenService.rotateRefreshToken(existing)).thenReturn(existing);
        when(jwtTokenProvider.generateTokenFromUserDetails(any(JwtUserDetails.class)))
            .thenReturn("new-access-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(900000L);

        TokenRefreshResponse response = authenticationService.refreshToken(
            new TokenRefreshRequest(" old-refresh ")
        );

        assertEquals("new-access-token", response.accessToken());
        assertEquals("rotated-refresh", response.refreshToken());
        assertEquals(900L, response.expiresIn());
        verify(refreshTokenService).rotateRefreshToken(existing);
    }

    @Test
    void refreshTokenRejectsBlankRequest() {
        assertThrows(
            BadRequestException.class,
            () -> authenticationService.refreshToken(new TokenRefreshRequest(" "))
        );
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void logoutRevokesRefreshTokenAndClearsSecurityContext() {
        authenticationService.logout(new LogoutRequest(" refresh-token "));

        verify(refreshTokenService).revokeRefreshToken("refresh-token");
        assertNull(org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication());
    }
}
