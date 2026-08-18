package org.paymentgateway.auth.dto.response;

import java.util.List;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    Long id,
    String username,
    String email,
    List<String> roles
) {
    public static AuthResponse of(
        String accessToken,
        String refreshToken,
        long expiresIn,
        Long id,
        String username,
        String email,
        List<String> roles
    ) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, id, username, email, roles);
    }
}
