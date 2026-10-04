package com.paymentgateway.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.payment.document.PaymentEventDocument;
import com.paymentgateway.payment.enums.PaymentStatus;
import com.paymentgateway.payment.model.PaymentEvent;
import com.paymentgateway.payment.repository.PaymentEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentKafkaProducerTest {

    @Test
    void persistsSanitizedEventInMongoBeforePublishingToKafka() throws Exception {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        PaymentEventRepository eventRepository = mock(PaymentEventRepository.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(new CompletableFuture<>());
        PaymentKafkaProducer producer = new PaymentKafkaProducer(
            kafkaTemplate, new ObjectMapper().findAndRegisterModules(), eventRepository
        );
        ReflectionTestUtils.setField(producer, "topicPaymentInitiated", "payment.initiated");

        PaymentEvent event = new PaymentEvent();
        event.setTransactionId("tx-1");
        event.setUserId(12L);
        event.setUserEmail("customer@example.com");
        event.setAmount(new BigDecimal("45.00"));
        event.setCurrency("EUR");
        event.setStatus(PaymentStatus.INITIATED);
        event.setRawCardNumber("4111111111111111");
        event.setCvv("123");
        event.setExpiryMonth("12");
        event.setExpiryYear("2030");

        producer.sendPaymentInitiated(event);

        var documentCaptor = org.mockito.ArgumentCaptor.forClass(PaymentEventDocument.class);
        verify(eventRepository).save(documentCaptor.capture());
        PaymentEventDocument stored = documentCaptor.getValue();
        assertEquals("tx-1", stored.getTransactionId());
        assertEquals("PAYMENT_INITIATED", stored.getEventType());
        assertEquals("EUR", stored.getCurrency());
        assertEquals(PaymentStatus.INITIATED, stored.getStatus());
        assertEquals(12L, stored.getPayload().userId());
        assertEquals("customer@example.com", stored.getPayload().email());
        assertNull(stored.getPayment().getRawCardNumber());
        assertNull(stored.getPayment().getCvv());
        assertNull(stored.getPayment().getExpiryMonth());
        assertNull(stored.getPayment().getExpiryYear());

        var messageCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("payment.initiated"), eq("tx-1"), messageCaptor.capture());
        var json = new ObjectMapper().readTree(messageCaptor.getValue());
        assertEquals(12, json.path("payload").path("userId").asInt());
        assertEquals("customer@example.com", json.path("payload").path("email").asText());
        assertEquals("tx-1", json.path("payment").path("transactionId").asText());
        assertEquals("EUR", json.path("payment").path("currency").asText());
        assertFalse(json.has("userId"));
        assertFalse(json.has("transactionId"));

        PaymentEvent restored = new ObjectMapper().findAndRegisterModules()
            .readValue(messageCaptor.getValue(), PaymentEvent.class);
        assertEquals(12L, restored.getUserId());
        assertEquals("customer@example.com", restored.getUserEmail());
        assertEquals("tx-1", restored.getTransactionId());
        assertEquals(new BigDecimal("45.00"), restored.getAmount());
        assertEquals("4111111111111111", restored.getRawCardNumber());
    }
}
