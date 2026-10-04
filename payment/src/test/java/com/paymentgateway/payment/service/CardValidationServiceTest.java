package com.paymentgateway.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.paymentgateway.payment.enums.CardProvider;

import static org.junit.jupiter.api.Assertions.*;

class   CardValidationServiceTest {

    private CardValidationService service;

    @BeforeEach
    void setUp() {
        service = new CardValidationService();
    }

    @Test
    @DisplayName("Valid Luhn credit/debit card numbers should pass")
    void testValidLuhn() {
        // Standard Luhn test card numbers
        assertTrue(service.isValidLuhn("4532015112830366")); // Valid Visa
        assertTrue(service.isValidLuhn("4242424242424242")); // Visa
        assertTrue(service.isValidLuhn("4000056655665556")); // Visa
        assertTrue(service.isValidLuhn("5555555555554444")); // Valid Mastercard
        assertTrue(service.isValidLuhn("2223003122003222")); // Mastercard 2-series
        assertTrue(service.isValidLuhn("378282246310005")); // American Express
        assertTrue(service.isValidLuhn("6011111111111117")); // Discover
        assertTrue(service.isValidLuhn("4111 1111 1111 1111")); // Visa with spaces
    }

    @Test
    @DisplayName("Corrupted card numbers failing Luhn checksum must return false")
    void testInvalidLuhn() {
        assertFalse(service.isValidLuhn("4532015112830367")); // Off by 1
        assertFalse(service.isValidLuhn("1234567812345678"));
        assertFalse(service.isValidLuhn("abc"));
        assertFalse(service.isValidLuhn(null));
    }

    @Test
    @DisplayName("Card provider prefixes should be detected accurately")
    void testDetectCardProvider() {
        assertEquals(CardProvider.VISA, service.detectCardProvider("4532015112830366"));
        assertEquals(CardProvider.MASTERCARD, service.detectCardProvider("5424180234567891"));
        assertEquals(CardProvider.AMEX, service.detectCardProvider("378282246310005"));
        assertEquals(CardProvider.DISCOVER, service.detectCardProvider("6011111111111117"));
        assertEquals(CardProvider.UNKNOWN, service.detectCardProvider("1234567890123456"));
    }

    @Test
    @DisplayName("Expiration dates in future should pass, past dates should fail")
    void testExpiryDates() {
        assertTrue(service.isNotExpired("12", "2035"));
        assertTrue(service.isNotExpired("01", "35"));
        assertFalse(service.isNotExpired("01", "2020"));
        assertFalse(service.isNotExpired("invalid", "year"));
    }

    @Test
    @DisplayName("CVV length validation matches card provider specification")
    void testCvvValidation() {
        assertTrue(service.isValidCvv("123", CardProvider.VISA));
        assertTrue(service.isValidCvv("456", CardProvider.MASTERCARD));
        assertTrue(service.isValidCvv("1234", CardProvider.AMEX));
        assertFalse(service.isValidCvv("12", CardProvider.VISA));
        assertFalse(service.isValidCvv("1234", CardProvider.VISA));
        assertFalse(service.isValidCvv("123", CardProvider.AMEX));
    }
}
