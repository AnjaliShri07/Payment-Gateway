package org.paymentgateway.auth.dto;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn) {}