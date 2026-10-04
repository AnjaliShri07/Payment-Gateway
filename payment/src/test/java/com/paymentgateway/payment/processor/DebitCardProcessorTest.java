package com.paymentgateway.payment.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.paymentgateway.payment.enums.CardType;
import com.paymentgateway.payment.model.PaymentEvent;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DebitCardProcessorTest {

    private DebitCardProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new DebitCardProcessor();
    }

    @Test
    @DisplayName("Debit processor returns CardType.DEBIT")
    void testSupportedType() {
        assertEquals(CardType.DEBIT, processor.getSupportedCardType());
    }

    @Test
    @DisplayName("CNP debit payment within daily limits approves with AuthCode and ARN")
    void testDebitApproval() {
        PaymentEvent event = new PaymentEvent();
        event.setTransactionId("tx-deb-001");
        event.setAmount(new BigDecimal("250.00"));
        event.setCurrency("USD");
        event.setCardLastFour("1234");
        event.setRawCardNumber("4000001234561234");
        event.setCardHolderName("John Doe");

        PaymentProcessingResult result = processor.process(event);

        assertTrue(result.success());
        assertNotNull(result.authCode());
        assertTrue(result.authCode().startsWith("DEBIT-AUTH-"));
        assertNotNull(result.acquirerReferenceNumber());
        assertTrue(result.acquirerReferenceNumber().startsWith("ARN-DEB-"));
    }

    @Test
    @DisplayName("Debit payment exceeding daily limit must be declined")
    void testDebitExceedsDailyLimit() {
        PaymentEvent event = new PaymentEvent();
        event.setTransactionId("tx-deb-002");
        event.setAmount(new BigDecimal("55000.00")); // Exceeds 50,000 limit
        event.setCurrency("USD");
        event.setCardLastFour("1234");

        PaymentProcessingResult result = processor.process(event);

        assertFalse(result.success());
        assertTrue(result.failureReason().contains("exceeds maximum daily withdrawal limit"));
    }
}
