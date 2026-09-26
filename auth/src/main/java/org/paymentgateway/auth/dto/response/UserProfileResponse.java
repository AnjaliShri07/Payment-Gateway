package org.paymentgateway.auth.dto.response;

import java.time.Instant;
import java.util.List;

/**

 * Response payload returned by authentication service operations.

 */
public record UserProfileResponse(
    Long id,
    String username,
    String email,
    List<String> roles,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt
) {}
