package org.paymentgateway.user.client.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO describing the authenticated user profile returned by the auth service.
 */
public record AuthUserProfile(
    Long id,
    String username,
    String email,
    List<String> roles,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt
) {}
