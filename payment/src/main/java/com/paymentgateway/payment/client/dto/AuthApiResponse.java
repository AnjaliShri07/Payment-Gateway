package com.paymentgateway.payment.client.dto;

import java.time.Instant;

public record AuthApiResponse<T>(
    boolean success,
    String message,
    T data,
    Instant timestamp
) {}
