package org.paymentgateway.auth.dto.response;

public record ClientTokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    String scope
) {}
