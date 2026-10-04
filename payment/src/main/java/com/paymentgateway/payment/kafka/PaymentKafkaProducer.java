package com.paymentgateway.payment.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.payment.document.PaymentEventDocument;
import com.paymentgateway.payment.model.PaymentEvent;
import com.paymentgateway.payment.repository.PaymentEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentKafkaProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentEventRepository paymentEventRepository;

    @Value("${app.kafka.topics.payment-initiated:payment.initiated}")
    private String topicPaymentInitiated;

    @Value("${app.kafka.topics.payment-completed:payment.completed}")
    private String topicPaymentCompleted;

    @Value("${app.kafka.topics.payment-failed:payment.failed}")
    private String topicPaymentFailed;

    @Value("${app.kafka.topics.payment-refunded:payment.refunded}")
    private String topicPaymentRefunded;

    public PaymentKafkaProducer(
        KafkaTemplate<String, String> kafkaTemplate,
        ObjectMapper objectMapper,
        PaymentEventRepository paymentEventRepository
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.paymentEventRepository = paymentEventRepository;
    }

    public void sendPaymentInitiated(PaymentEvent event) {
        send(topicPaymentInitiated, event.getTransactionId(), event, "PAYMENT_INITIATED");
    }

    public void sendPaymentCompleted(PaymentEvent event) {
        send(topicPaymentCompleted, event.getTransactionId(), event, "PAYMENT_COMPLETED");
    }

    public void sendPaymentFailed(PaymentEvent event) {
        send(topicPaymentFailed, event.getTransactionId(), event, "PAYMENT_FAILED");
    }

    public void sendPaymentRefunded(PaymentEvent event) {
        send(topicPaymentRefunded, event.getTransactionId(), event, "PAYMENT_REFUNDED");
    }

    private void send(String topic, String key, PaymentEvent event, String eventType) {
        try {
            paymentEventRepository.save(PaymentEventDocument.from(event, eventType));
            String jsonPayload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, jsonPayload)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Sent event for transaction [{}] to topic [{}], partition [{}], offset [{}]",
                            key, topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send event for transaction [{}] to topic [{}]: {}",
                            key, topic, ex.getMessage());
                    }
                });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize payment event for transaction " + key, e);
        }
    }
}
