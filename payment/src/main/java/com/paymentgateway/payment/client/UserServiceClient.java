package com.paymentgateway.payment.client;

import com.paymentgateway.payment.client.dto.UserBaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Service that communicates with the User microservice (User service at port 8081).
 * Forwarding JWT token in the Authorization header to retrieve and verify user details.
 */
@Service
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserServiceClient(
        RestTemplate restTemplate,
        @Value("${app.services.user-service-url:http://localhost:8081}") String userServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    /**
     * Retrieves user details from the User microservice by user ID, passing the bearer token.
     *
     * @param userId user identifier
     * @param bearerToken JWT token to forward
     * @return Optional containing user data map if found
     */
    public Optional<Map<String, Object>> getUserById(Long userId, String bearerToken) {
        if (userId == null) {
            return Optional.empty();
        }

        HttpHeaders headers = new HttpHeaders();
        if (bearerToken != null && !bearerToken.isBlank()) {
            String authHeader = bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken.trim();
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = userServiceUrl + "/api/v1/users/" + userId;

        try {
            ResponseEntity<UserBaseResponse<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<UserBaseResponse<Map<String, Object>>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.ofNullable(response.getBody().getData());
            }
        } catch (RestClientException ex) {
            log.warn("Could not fetch user info from User microservice at {}: {}", url, ex.getMessage());
        }

        return Optional.empty();
    }
}
