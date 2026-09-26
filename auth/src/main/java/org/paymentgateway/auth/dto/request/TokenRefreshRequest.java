package org.paymentgateway.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**

 * Request payload for authentication service operations.

 */
public record TokenRefreshRequest(
    @NotBlank(message = "Refresh token cannot be blank")
    String refreshToken
) {}
