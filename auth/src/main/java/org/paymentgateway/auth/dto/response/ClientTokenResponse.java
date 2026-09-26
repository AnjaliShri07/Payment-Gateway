package org.paymentgateway.auth.dto.response;

/**

 * Response payload returned by authentication service operations.

 */
public record ClientTokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    String scope
) {}
