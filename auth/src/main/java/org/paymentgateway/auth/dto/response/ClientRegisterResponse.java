package org.paymentgateway.auth.dto.response;

public record ClientRegisterResponse(
    String clientId,
    String clientSecret
) {}
