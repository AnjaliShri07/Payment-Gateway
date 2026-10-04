package com.paymentgateway.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error payload returned by payment service operations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> validationErrors
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponse of(
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
    ) {
        return new ErrorResponse(Instant.now(), status, error, message, path, validationErrors);
    }
}
