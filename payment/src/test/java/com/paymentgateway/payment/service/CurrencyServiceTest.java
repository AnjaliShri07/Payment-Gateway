package com.paymentgateway.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyServiceTest {

    private CurrencyService currencyService;

    @BeforeEach
    void setUp() {
        currencyService = new CurrencyService();
    }

    @Test
    @DisplayName("Verify supported currency detection")
    void testSupportedCurrencies() {
        assertTrue(currencyService.isSupportedCurrency("USD"));
        assertTrue(currencyService.isSupportedCurrency("EUR"));
        assertTrue(currencyService.isSupportedCurrency("inr"));
        assertFalse(currencyService.isSupportedCurrency("GBP"));
        assertFalse(currencyService.isSupportedCurrency("XYZ"));
        assertFalse(currencyService.isSupportedCurrency(null));
    }

    @Test
    @DisplayName("Verify currency conversion calculations")
    void testConversion() {
        BigDecimal usd = new BigDecimal("100.00");
        BigDecimal converted = currencyService.convert(usd, "USD", "INR");
        assertNotNull(converted);
        assertTrue(converted.compareTo(new BigDecimal("8000.00")) > 0);
    }

    @Test
    @DisplayName("Rejects currencies outside INR, USD, and EUR")
    void testUnsupportedCurrencyConversionRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> currencyService.convert(new BigDecimal("10.00"), "USD", "GBP"));
    }
}
