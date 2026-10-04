package com.paymentgateway.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.payment.model.PaymentEvent;
import com.paymentgateway.payment.service.WebhookSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Listens on payment.completed, payment.failed, and payment.refunded topics
 * to send customer notifications / dispatch HMAC-SHA256 signed webhooks.
 */
@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final ObjectMapper objectMapper;
    private final WebhookSecurityService webhookSecurityService;

    public NotificationConsumer(ObjectMapper objectMapper, WebhookSecurityService webhookSecurityService) {
        this.objectMapper = objectMapper;
        this.webhookSecurityService = webhookSecurityService;
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-completed:payment.completed}", groupId = "notification-service-group")
    public void onPaymentCompleted(String message) {
        try {
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
            String signature = webhookSecurityService.generateSignature(message);
            log.info("🔔 [NOTIFICATION] Payment COMPLETED successfully! TxId: {}, User: {}, Amount: {} {}, Card: **** {}, AuthCode: {}, HMAC: {}",
                event.getTransactionId(), event.getUserEmail() != null ? event.getUserEmail() : event.getUserId(),
                event.getAmount(), event.getCurrency(), event.getCardLastFour(), event.getAuthCode(), signature);
        } catch (Exception e) {
            log.error("Error parsing completed event in NotificationConsumer: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-failed:payment.failed}", groupId = "notification-service-group")
    public void onPaymentFailed(String message) {
        try {
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
            String signature = webhookSecurityService.generateSignature(message);
            log.warn("⚠️ [NOTIFICATION] Payment FAILED! TxId: {}, User: {}, Amount: {} {}, Reason: {}, HMAC: {}",
                event.getTransactionId(), event.getUserEmail() != null ? event.getUserEmail() : event.getUserId(),
                event.getAmount(), event.getCurrency(), event.getFailureReason(), signature);
        } catch (Exception e) {
            log.error("Error parsing failed event in NotificationConsumer: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-refunded:payment.refunded}", groupId = "notification-service-group")
    public void onPaymentRefunded(String message) {
        try {
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
            String signature = webhookSecurityService.generateSignature(message);
            log.info("↩️ [NOTIFICATION] Payment REFUNDED! TxId: {}, RefundAmount: {} {}, Reason: {}, HMAC: {}",
                event.getTransactionId(), event.getRefundAmount(), event.getCurrency(), event.getRefundReason(), signature);
        } catch (Exception e) {
            log.error("Error parsing refunded event in NotificationConsumer: {}", e.getMessage());
        }
    }
}
