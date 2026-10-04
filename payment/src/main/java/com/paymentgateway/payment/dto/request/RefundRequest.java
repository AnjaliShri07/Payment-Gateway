package com.paymentgateway.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RefundRequest(
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
    BigDecimal amount,

    @Size(max = 255, message = "Refund reason cannot exceed 255 characters")
    String reason
) {}
