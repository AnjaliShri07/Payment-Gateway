package com.paymentgateway.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Webhook & Event Integrity Security Service:
 * Signs outgoing webhook payloads and event notifications using HMAC-SHA256,
 * allowing merchants to cryptographically verify payload origin and integrity.
 */
@Service
public class WebhookSecurityService {

    private static final Logger log = LoggerFactory.getLogger(WebhookSecurityService.class);
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    @Value("${app.webhook.secret:payment_gateway_webhook_hmac_secret_2026_key}")
    private String webhookSecret;

    /**
     * Generates a hex-encoded HMAC-SHA256 signature for a JSON payload.
     */
    public String generateSignature(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("Failed to generate HMAC-SHA256 webhook signature: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Verifies that the provided signature matches the payload HMAC.
     */
    public boolean verifySignature(String payload, String expectedSignature) {
        if (payload == null || expectedSignature == null) {
            return false;
        }
        String calculated = generateSignature(payload);
        return MessageDigest.isEqual(
            calculated.getBytes(StandardCharsets.UTF_8),
            expectedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
