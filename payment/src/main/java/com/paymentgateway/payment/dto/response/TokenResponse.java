package com.paymentgateway.payment.dto.response;


import com.paymentgateway.payment.enums.CardProvider;
import com.paymentgateway.payment.enums.CardType;

import java.time.Instant;

public record TokenResponse(
    String cardToken,
    String cardLastFour,
    CardProvider cardProvider,
    CardType cardType,
    String cardHolderName,
    String expiryMonth,
    String expiryYear,
    Instant createdAt
) {}
