package com.paymentgateway.payment.client.dto;

import java.time.Instant;
import java.util.List;

public record AuthUserProfile(
    Long id,
    String username,
    String email,
    List<String> roles,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt
) {}
