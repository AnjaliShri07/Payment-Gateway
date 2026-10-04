package com.paymentgateway.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class WebhookSecurityServiceTest {

    private WebhookSecurityService webhookSecurityService;

    @BeforeEach
    void setUp() {
        webhookSecurityService = new WebhookSecurityService();
        ReflectionTestUtils.setField(webhookSecurityService, "webhookSecret", "test_secret_key_123");
    }

    @Test
    @DisplayName("HMAC-SHA256 signature generation and verification succeeds for identical payload")
    void testHmacGenerationAndVerification() {
        String payload = "{\"transactionId\":\"tx-123\",\"amount\":500.00,\"status\":\"COMPLETED\"}";

        String signature = webhookSecurityService.generateSignature(payload);
        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        assertTrue(webhookSecurityService.verifySignature(payload, signature));
    }

    @Test
    @DisplayName("Tampered payload must fail signature verification")
    void testTamperedPayloadFails() {
        String payload = "{\"transactionId\":\"tx-123\",\"amount\":500.00,\"status\":\"COMPLETED\"}";
        String signature = webhookSecurityService.generateSignature(payload);

        String tampered = "{\"transactionId\":\"tx-123\",\"amount\":9999.00,\"status\":\"COMPLETED\"}";
        assertFalse(webhookSecurityService.verifySignature(tampered, signature));
    }
}
