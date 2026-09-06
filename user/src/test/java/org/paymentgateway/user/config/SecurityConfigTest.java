package org.paymentgateway.user.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paymentgateway.user.client.AuthenticationServiceClient;
import org.paymentgateway.user.client.dto.AuthUserProfile;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private AuthenticationServiceClient authenticationServiceClient;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenPopulatesSecurityContextAndContinuesChain() throws Exception {
        AuthUserProfile profile = new AuthUserProfile(
                1L, "alice", "alice@example.com", List.of("ROLE_USER"), true,
                Instant.now(), Instant.now());
        when(authenticationServiceClient.getAuthenticatedUser("Bearer token"))
                .thenReturn(Optional.of(profile));

        TestableTokenAuthenticationFilter filter =
                new TestableTokenAuthenticationFilter(authenticationServiceClient);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.invoke(request, response, filterChain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isSameAs(profile);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void missingBearerHeaderDoesNotCallAuthServiceButContinuesChain() throws Exception {
        TestableTokenAuthenticationFilter filter =
                new TestableTokenAuthenticationFilter(authenticationServiceClient);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.invoke(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(authenticationServiceClient);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidTokenDoesNotAuthenticateButContinuesChain() throws Exception {
        when(authenticationServiceClient.getAuthenticatedUser("Bearer invalid"))
                .thenReturn(Optional.empty());
        TestableTokenAuthenticationFilter filter =
                new TestableTokenAuthenticationFilter(authenticationServiceClient);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.invoke(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authenticationClientFailureDoesNotPreventChainContinuation() throws Exception {
        when(authenticationServiceClient.getAuthenticatedUser("Bearer broken"))
                .thenThrow(new RuntimeException("auth service down"));
        TestableTokenAuthenticationFilter filter =
                new TestableTokenAuthenticationFilter(authenticationServiceClient);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer broken");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.invoke(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static class TestableTokenAuthenticationFilter
            extends SecurityConfig.TokenAuthenticationFilter {

        private TestableTokenAuthenticationFilter(AuthenticationServiceClient client) {
            super(client);
        }

        private void invoke(
                MockHttpServletRequest request,
                MockHttpServletResponse response,
                FilterChain chain
        ) throws Exception {
            doFilterInternal(request, response, chain);
        }
    }
}
