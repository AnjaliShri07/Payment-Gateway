package com.paymentgateway.payment.dto.request;

import com.paymentgateway.payment.enums.CardType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payment initiation request.
 * Supports either direct card submission (which is immediately tokenized)
 * or a pre-generated PCI-DSS cardToken for returning customers.
 */
public record PaymentRequest(
    @NotNull(message = "Card type is required (DEBIT or CREDIT)")
    CardType cardType,

    /** Optional if cardToken is provided */
    String cardNumber,

    /** Optional: PCI-DSS token from previous tokenization */
    String cardToken,

    /** Required if cardNumber is provided; optional if cardToken is used */
    String cardHolderName,

    /** Required if cardNumber is provided (MM) */
    String expiryMonth,

    /** Required if cardNumber is provided (YY or YYYY) */
    String expiryYear,

    /** Required if cardNumber is provided */
    String cvv,

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    BigDecimal amount,

    @NotBlank(message = "Currency is required (e.g. USD, EUR, INR)")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    String currency,

    String description,

    /** Online 3D Secure / SCA simulation flag */
    Boolean threeDSecureRequired
) {}
