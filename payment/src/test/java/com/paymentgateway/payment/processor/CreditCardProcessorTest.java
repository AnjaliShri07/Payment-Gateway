package com.paymentgateway.payment.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import com.paymentgateway.payment.enums.CardProvider;
import com.paymentgateway.payment.enums.CardType;
import com.paymentgateway.payment.model.PaymentEvent;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CreditCardProcessorTest {

    private CreditCardProcessor processor;

    @BeforeEach
    void setUp() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.nextSpan()).thenReturn(span);
        when(span.name(anyString())).thenReturn(span);
        when(span.tag(anyString(), anyString())).thenReturn(span);
        when(span.start()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("test-trace-id");
        when(tracer.withSpan(span)).thenReturn(mock(Tracer.SpanInScope.class));
        processor = new CreditCardProcessor(tracer);
    }

    @Test
    @DisplayName("Credit processor returns CardType.CREDIT")
    void testSupportedType() {
        assertEquals(CardType.CREDIT, processor.getSupportedCardType());
    }

    @Test
    @DisplayName("Credit payment within credit ceiling approves with Network AuthCode and ARN")
    void testCreditApproval() {
        PaymentEvent event = new PaymentEvent();
        event.setTransactionId("tx-crd-001");
        event.setAmount(new BigDecimal("1500.00"));
        event.setCurrency("USD");
        event.setCardProvider(CardProvider.VISA);
        event.setCardLastFour("4321");
        event.setCardHolderName("Alice Smith");

        PaymentProcessingResult result = processor.process(event);

        assertTrue(result.success());
        assertNotNull(result.authCode());
        assertTrue(result.authCode().startsWith("CREDIT-AUTH-"));
        assertNotNull(result.acquirerReferenceNumber());
        assertTrue(result.acquirerReferenceNumber().startsWith("ARN-CRD-"));
    }

    @Test
    @DisplayName("Credit payment exceeding maximum single charge ceiling must be declined")
    void testCreditExceedsCeiling() {
        PaymentEvent event = new PaymentEvent();
        event.setTransactionId("tx-crd-002");
        event.setAmount(new BigDecimal("120000.00")); // Exceeds 100,000 limit
        event.setCurrency("USD");
        event.setCardProvider(CardProvider.MASTERCARD);

        PaymentProcessingResult result = processor.process(event);

        assertFalse(result.success());
        assertTrue(result.failureReason().contains("exceeds maximum single charge limit"));
    }
}
