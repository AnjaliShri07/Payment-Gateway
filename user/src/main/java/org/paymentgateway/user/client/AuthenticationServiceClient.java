package org.paymentgateway.user.client;

import org.paymentgateway.user.client.dto.AuthApiResponse;
import org.paymentgateway.user.client.dto.AuthUserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Client used to validate bearer tokens and load the current authenticated user
 * details from the authentication service.
 */
@Service
public class AuthenticationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceClient.class);


    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    public AuthenticationServiceClient(
            RestTemplate restTemplate,
            @Value("${app.services.auth-service-url}") String authServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl;
    }

    /**
     * Validates the bearer token against the Auth microservice and retrieves the caller's profile.
     *
     * @param bearerToken JWT bearer token (with or without 'Bearer ' prefix)
     * @return Optional containing AuthUserProfile if valid, empty otherwise
     */
    public Optional<AuthUserProfile> getAuthenticatedUser(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return Optional.empty();
        }

        String authHeader = bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken.trim();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = authServiceUrl + "/api/v1/users/me";

        try {
            ResponseEntity<AuthApiResponse<AuthUserProfile>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.ofNullable(response.getBody().data());
            }
        } catch (RestClientException ex) {
            log.error("Failed to authenticate token with Auth microservice at {}: {}", url, ex.getMessage());
        }

        return Optional.empty();
    }
}
