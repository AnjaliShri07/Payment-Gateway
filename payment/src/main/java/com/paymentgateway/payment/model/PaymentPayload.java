package com.paymentgateway.payment.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentPayload(
    Long userId,
    String username,
    String email,
    List<String> roles,
    Boolean enabled,
    Instant createdAt,
    Instant updatedAt
) {}
