package org.paymentgateway.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**

 * Request payload for authentication service operations.

 */
public record ClientTokenRequest(
    @NotBlank(message = "clientId is required")
    String clientId,
    @NotBlank(message = "clientSecret is required")
    String clientSecret,
    String grantType // optional, support client_credentials
) {}
