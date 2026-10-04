package com.paymentgateway.payment.service;

import com.paymentgateway.payment.enums.CardProvider;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.regex.Pattern;

@Service
public class CardValidationService {

    private static final Pattern VISA_PATTERN = Pattern.compile("^4[0-9]{12}(?:[0-9]{3})?$");
    private static final Pattern MASTERCARD_PATTERN = Pattern.compile("^(?:5[1-5][0-9]{2}|222[1-9]|22[3-9][0-9]|2[3-6][0-9]{2}|27[01][0-9]|2720)[0-9]{12}$");
    private static final Pattern AMEX_PATTERN = Pattern.compile("^3[47][0-9]{13}$");
    private static final Pattern DISCOVER_PATTERN = Pattern.compile("^6(?:011|5[0-9]{2})[0-9]{12}$");

    /**
     * Luhn algorithm validation for credit/debit card numbers.
     */
    public boolean isValidLuhn(String cardNumber) {
        if (cardNumber == null) return false;
        String clean = cardNumber.replaceAll("\\s+", "");
        if (!clean.matches("^[0-9]{13,19}$")) return false;

        int sum = 0;
        boolean alternate = false;
        for (int i = clean.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(clean.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    /**
     * Detects card provider from card number prefix.
     */
    public CardProvider detectCardProvider(String cardNumber) {
        if (cardNumber == null) return CardProvider.UNKNOWN;
        String clean = cardNumber.replaceAll("\\s+", "");

        if (VISA_PATTERN.matcher(clean).matches()) return CardProvider.VISA;
        if (MASTERCARD_PATTERN.matcher(clean).matches()) return CardProvider.MASTERCARD;
        if (AMEX_PATTERN.matcher(clean).matches()) return CardProvider.AMEX;
        if (DISCOVER_PATTERN.matcher(clean).matches()) return CardProvider.DISCOVER;

        return CardProvider.UNKNOWN;
    }

    /**
     * Checks if card expiration month and year are valid and in the future.
     */
    public boolean isNotExpired(String expiryMonth, String expiryYear) {
        try {
            int month = Integer.parseInt(expiryMonth.trim());
            int year = Integer.parseInt(expiryYear.trim());

            if (year < 100) {
                year += 2000;
            }

            YearMonth cardExpiry = YearMonth.of(year, month);
            YearMonth now = YearMonth.now();

            return !cardExpiry.isBefore(now);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates CVV length according to provider (AMEX: 4 digits, others: 3 digits).
     */
    public boolean isValidCvv(String cvv, CardProvider provider) {
        if (cvv == null) return false;
        String clean = cvv.trim();
        if (provider == CardProvider.AMEX) {
            return clean.matches("^[0-9]{4}$");
        }
        return clean.matches("^[0-9]{3}$");
    }
}
