package com.paymentgateway.payment.dto.request;

import com.paymentgateway.payment.enums.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TokenizeCardRequest(
    @NotNull(message = "Card type is required (DEBIT or CREDIT)")
    CardType cardType,

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Card number must be 13 to 19 digits")
    String cardNumber,

    @NotBlank(message = "Card holder name is required")
    @Size(min = 2, max = 100, message = "Card holder name must be between 2 and 100 characters")
    String cardHolderName,

    @NotBlank(message = "Expiry month is required (MM)")
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Expiry month must be MM (01-12)")
    String expiryMonth,

    @NotBlank(message = "Expiry year is required (YY or YYYY)")
    @Pattern(regexp = "^(20[2-9][0-9]|[2-9][0-9])$", message = "Expiry year must be in the future")
    String expiryYear,

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must be 3 or 4 digits")
    String cvv
) {}
