package org.paymentgateway.user.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.paymentgateway.user.client.dto.AuthApiResponse;
import org.paymentgateway.user.client.dto.AuthUserProfile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private AuthenticationServiceClient client;

    @BeforeEach
    void setUp() {
        client = new AuthenticationServiceClient(restTemplate, "http://auth-service");
    }

    @ParameterizedTest
    @CsvSource({
            "''",
            "'   '"
    })
    void blankTokensReturnEmptyWithoutCallingAuthService(String token) {
        assertThat(client.getAuthenticatedUser(token)).isEmpty();

        verifyNoInteractions(restTemplate);
    }

    @Test
    void successfulResponseReturnsProfileAndNormalizesBearerHeader() {
        AuthUserProfile profile = profile();
        AuthApiResponse<AuthUserProfile> body =
                new AuthApiResponse<>(true, "ok", profile, Instant.now());
        ResponseEntity<AuthApiResponse<AuthUserProfile>> response = ResponseEntity.ok(body);

        when(restTemplate.exchange(
                eq("http://auth-service/api/v1/users/me"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<AuthApiResponse<AuthUserProfile>>>any()
        )).thenReturn(response);

        assertThat(client.getAuthenticatedUser("token-value")).contains(profile);

        verify(restTemplate).exchange(
                eq("http://auth-service/api/v1/users/me"),
                eq(HttpMethod.GET),
                argThat(entity -> "Bearer token-value".equals(
                        entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))),
                ArgumentMatchers.<ParameterizedTypeReference<AuthApiResponse<AuthUserProfile>>>any()
        );
    }

    @Test
    void existingBearerPrefixIsNotDuplicated() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<AuthApiResponse<AuthUserProfile>>>any()
        )).thenReturn(ResponseEntity.ok(new AuthApiResponse<>(true, "ok", profile(), Instant.now())));

        client.getAuthenticatedUser("Bearer token-value");

        verify(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.GET),
                argThat(entity -> "Bearer token-value".equals(
                        entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))),
                ArgumentMatchers.<ParameterizedTypeReference<AuthApiResponse<AuthUserProfile>>>any()
        );
    }

    @Test
    void nonSuccessResponseReturnsEmpty() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<AuthApiResponse<AuthUserProfile>>>any()
        )).thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        assertThat(client.getAuthenticatedUser("token")).isEmpty();
    }

    @Test
    void nullBodyReturnsEmpty() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<AuthApiResponse<AuthUserProfile>>>any()
        )).thenReturn(ResponseEntity.ok().build());

        assertThat(client.getAuthenticatedUser("token")).isEmpty();
    }

    @Test
    void restClientFailureReturnsEmpty() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<AuthApiResponse<AuthUserProfile>>>any()
        )).thenThrow(new RestClientException("service unavailable"));

        assertThat(client.getAuthenticatedUser("token")).isEqualTo(Optional.empty());
    }

    private AuthUserProfile profile() {
        return new AuthUserProfile(1L, "alice", "alice@example.com",
                List.of("ROLE_USER"), true, Instant.now(), Instant.now());
    }
}
