package org.paymentgateway.auth;

import org.junit.jupiter.api.Test;
import org.paymentgateway.auth.entity.JwtUser;
import org.paymentgateway.auth.entity.Role;
import org.paymentgateway.auth.repository.UserRepository;
import org.paymentgateway.auth.security.JwtUserDetails;
import org.paymentgateway.auth.security.JwtUserDetailsService;
import org.paymentgateway.auth.security.jwt.JwtTokenProvider;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtSecurityTests {

    @Mock
    private UserRepository userRepository;

    @Test
    void userDetailsServiceLoadsUserByUsernameOrEmail() {
        JwtUser user = new JwtUser();
        user.setId(4L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPassword("hashed");
        user.setRoles(Set.of(new Role(org.paymentgateway.auth.constants.ERole.ROLE_USER)));
        when(userRepository.findByUsernameOrEmail("alice@example.com", "alice@example.com"))
            .thenReturn(Optional.of(user));

        var service = new JwtUserDetailsService(userRepository);
        JwtUserDetails details = (JwtUserDetails) service.loadUserByUsername(" alice@example.com ");

        assertEquals(4L, details.getId());
        assertEquals("alice", details.getUsername());
        assertEquals(List.of("ROLE_USER"),
            details.getAuthorities().stream().map(Object::toString).toList());
    }

    @Test
    void tokenProviderGeneratesAndValidatesJwt() {
        JwtTokenProvider provider = new JwtTokenProvider(
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
            900000L
        );
        JwtUserDetails details = new JwtUserDetails(
            4L,
            "alice",
            "alice@example.com",
            "hashed",
            List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")),
            true,
            true,
            true,
            true
        );

        String token = provider.generateTokenFromUserDetails(details);

        assertTrue(provider.validateJwtToken(token));
        assertEquals("alice", provider.getUsernameFromJwtToken(token));
        assertEquals(4L, provider.getUserIdFromJwtToken(token));
        assertEquals(List.of("ROLE_USER"), provider.getRolesFromJwtToken(token));
    }

    @Test
    void tokenProviderRejectsBlankToken() {
        JwtTokenProvider provider = new JwtTokenProvider(
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
            900000L
        );

        assertFalse(provider.validateJwtToken(""));
        assertNull(provider.getUsernameFromJwtToken(null));
    }
}
