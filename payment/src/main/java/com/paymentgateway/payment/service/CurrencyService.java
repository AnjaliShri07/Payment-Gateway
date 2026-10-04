package com.paymentgateway.payment.service;

import com.paymentgateway.payment.enums.Currency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Multi-Currency & Cross-Border Payment Service:
 * Validates ISO 4217 currency codes and computes exchange rates against USD.
 */
@Service
public class CurrencyService {

    // Exchange rates relative to USD (1.00 USD = X)
    private static final Map<Currency, BigDecimal> RATES_TO_USD = createRatesToUsd();

    private static Map<Currency, BigDecimal> createRatesToUsd() {
        Map<Currency, BigDecimal> rates = new EnumMap<>(Currency.class);
        rates.put(Currency.USD, new BigDecimal("1.00"));
        rates.put(Currency.EUR, new BigDecimal("0.92"));
        rates.put(Currency.INR, new BigDecimal("83.50"));
        return Map.copyOf(rates);
    }

    public boolean isSupportedCurrency(String currency) {
        return Currency.fromCode(currency).map(RATES_TO_USD::containsKey).orElse(false);
    }

    public Set<String> getSupportedCurrencies() {
        return RATES_TO_USD.keySet().stream()
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Converts an amount from one currency to USD.
     */
    public BigDecimal convertToUsd(BigDecimal amount, String fromCurrency) {
        Currency currency = requireSupportedCurrency(fromCurrency);
        BigDecimal rate = RATES_TO_USD.get(currency);
        if (currency == Currency.USD) {
            return amount;
        }
        return amount.divide(rate, 2, RoundingMode.HALF_UP);
    }

    /**
     * Converts an amount from source currency to target currency.
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        Currency from = requireSupportedCurrency(fromCurrency);
        Currency to = requireSupportedCurrency(toCurrency);

        if (from.equals(to)) {
            return amount;
        }

        BigDecimal inUsd = convertToUsd(amount, from.name());
        BigDecimal targetRate = RATES_TO_USD.get(to);
        return inUsd.multiply(targetRate).setScale(2, RoundingMode.HALF_UP);
    }

    private Currency requireSupportedCurrency(String currency) {
        return Currency.fromCode(currency)
            .filter(RATES_TO_USD::containsKey)
            .orElseThrow(() -> new IllegalArgumentException("Unsupported currency: " + currency
                + ". Supported currencies are: " + getSupportedCurrencies()));
    }
}
