package com.paymentgateway.payment.dto.response;


import com.paymentgateway.payment.enums.CardProvider;
import com.paymentgateway.payment.enums.CardType;
import com.paymentgateway.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
    String transactionId,
    Long userId,
    String idempotencyKey,
    String cardHolderName,
    String cardLastFour,
    String cardToken,
    CardType cardType,
    CardProvider cardProvider,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    String authCode,
    String acquirerReferenceNumber,
    Integer riskScore,
    String description,
    String failureReason,
    BigDecimal refundAmount,
    String refundReason,
    Instant createdAt,
    Instant processedAt,
    Instant refundedAt
) {}
