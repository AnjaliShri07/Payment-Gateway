package org.paymentgateway.user.client.dto;

import java.time.Instant;

/**
 * Generic response wrapper returned by the authentication service.
 *
 * @param <T> payload type included in the response body
 */
public record AuthApiResponse<T>(
    boolean success,
    String message,
    T data,
    Instant timestamp
) {}
