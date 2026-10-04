package com.paymentgateway.payment.enums;

import java.util.Locale;
import java.util.Optional;

public enum Currency {
    INR,
    USD,
    EUR;

    public static Optional<Currency> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(valueOf(code.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
