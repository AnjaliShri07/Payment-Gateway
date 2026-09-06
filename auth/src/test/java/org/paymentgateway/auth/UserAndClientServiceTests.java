package org.paymentgateway.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.paymentgateway.auth.dto.request.ClientRegisterRequest;
import org.paymentgateway.auth.dto.request.ClientTokenRequest;
import org.paymentgateway.auth.entity.ClientApplication;
import org.paymentgateway.auth.entity.JwtUser;
import org.paymentgateway.auth.entity.Role;
import org.paymentgateway.auth.repository.ClientApplicationRepository;
import org.paymentgateway.auth.repository.UserRepository;
import org.paymentgateway.auth.security.JwtUserDetails;
import org.paymentgateway.auth.security.jwt.JwtTokenProvider;
import org.paymentgateway.auth.service.ClientApplicationService;
import org.paymentgateway.auth.service.UserService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAndClientServiceTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ClientApplicationRepository clientRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private UserService userService;
    private ClientApplicationService clientService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
        clientService = new ClientApplicationService(
            clientRepository,
            passwordEncoder,
            jwtTokenProvider
        );
    }

    @Test
    void getAllUsersMapsEntitiesToProfiles() {
        JwtUser user = new JwtUser();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setRoles(Set.of(new Role(org.paymentgateway.auth.constants.ERole.ROLE_ADMIN)));

        when(userRepository.findAll()).thenReturn(List.of(user));

        var profiles = userService.getAllUsers();

        assertEquals(1, profiles.size());
        assertEquals(1L, profiles.get(0).id());
        assertEquals("alice", profiles.get(0).username());
        assertEquals(List.of("ROLE_ADMIN"), profiles.get(0).roles());
    }

    @Test
    void getUserProfileRejectsNullId() {
        assertThrows(
            org.paymentgateway.auth.exception.BadRequestException.class,
            () -> userService.getUserProfile(null)
        );
        verifyNoInteractions(userRepository);
    }

    @Test
    void getUserProfileReturnsEmptyWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(userService.getUserProfile(99L).isEmpty());
    }

    @Test
    void getUserProfileByUsernameReturnsEmptyWhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertTrue(userService.getUserProfileByUsername(" missing ").isEmpty());
    }

    @Test
    void loadUserByUsernameReturnsEmptyWhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertTrue(userService.loadUserByUsername(" missing ").isEmpty());
    }

    @Test
    void loadUserByUsernameReturnsMatchingUser() {
        JwtUser user = new JwtUser();
        user.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertSame(user, userService.loadUserByUsername("alice").orElseThrow());
    }

    @Test
    void registerClientHashesSecretAndPersistsClient() {
        when(passwordEncoder.encode(any(String.class))).thenReturn("hashed-secret");

        var response = clientService.registerClient(
            new ClientRegisterRequest("Reporting", Set.of("read", "write"))
        );

        ArgumentCaptor<ClientApplication> captor = ArgumentCaptor.forClass(ClientApplication.class);
        verify(clientRepository).save(captor.capture());
        ClientApplication saved = captor.getValue();

        assertTrue(response.clientId().startsWith("client-"));
        assertNotNull(response.clientSecret());
        assertEquals("hashed-secret", saved.getClientSecret());
        assertEquals("Reporting", saved.getName());
        assertEquals(Set.of("read", "write"), saved.getScopes());
    }

    @Test
    void issueTokenRejectsInvalidClientSecret() {
        ClientApplication app = new ClientApplication();
        app.setClientId("client-1");
        app.setClientSecret("stored-hash");
        when(clientRepository.findByClientId("client-1")).thenReturn(Optional.of(app));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThrows(
            IllegalArgumentException.class,
            () -> clientService.issueToken(
                new ClientTokenRequest("client-1", "wrong", "client_credentials")
            )
        );

        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void issueTokenCreatesScopedAccessToken() {
        ClientApplication app = new ClientApplication();
        app.setId(3L);
        app.setClientId("client-1");
        app.setClientSecret("stored-hash");
        app.setName("Reporting");
        app.setScopes(Set.of("read", "write"));

        when(clientRepository.findByClientId("client-1")).thenReturn(Optional.of(app));
        when(passwordEncoder.matches("secret", "stored-hash")).thenReturn(true);
        when(jwtTokenProvider.generateTokenFromUserDetails(any(JwtUserDetails.class)))
            .thenReturn("client-access-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(900000L);

        var response = clientService.issueToken(
            new ClientTokenRequest("client-1", "secret", "client_credentials")
        );

        assertEquals("client-access-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());
        assertTrue(Set.of("read write", "write read").contains(response.scope()));
    }
}
