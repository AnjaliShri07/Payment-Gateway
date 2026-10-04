package com.paymentgateway.payment.service;

import com.paymentgateway.payment.dto.request.PaymentRequest;
import com.paymentgateway.payment.dto.request.RefundRequest;
import com.paymentgateway.payment.dto.response.PaymentAnalyticsResponse;
import com.paymentgateway.payment.dto.response.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.paymentgateway.payment.client.UserServiceClient;
import com.paymentgateway.payment.client.dto.AuthUserProfile;
import com.paymentgateway.payment.entity.PaymentTransaction;
import com.paymentgateway.payment.enums.CardProvider;
import com.paymentgateway.payment.enums.CardType;
import com.paymentgateway.payment.enums.PaymentStatus;
import com.paymentgateway.payment.exception.PaymentFailureException;
import com.paymentgateway.payment.kafka.PaymentKafkaProducer;
import com.paymentgateway.payment.model.PaymentEvent;
import com.paymentgateway.payment.repository.PaymentAuditLogRepository;
import com.paymentgateway.payment.repository.PaymentTransactionRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private PaymentAuditLogRepository auditLogRepository;

    @Mock
    private PaymentKafkaProducer kafkaProducer;

    @Mock
    private UserServiceClient userServiceClient;

    private CardValidationService cardValidationService;
    private TokenizationService tokenizationService;
    private CurrencyService currencyService;
    private PaymentService paymentService;

    private AuthUserProfile testUser;

    @BeforeEach
    void setUp() {
        cardValidationService = new CardValidationService();
        tokenizationService = new TokenizationService(cardValidationService);
        currencyService = new CurrencyService();

        paymentService = new PaymentService(
            transactionRepository,
            auditLogRepository,
            kafkaProducer,
            cardValidationService,
            tokenizationService,
            currencyService,
            userServiceClient
        );

        testUser = new AuthUserProfile(1L, "johndoe", "john@example.com", List.of("ROLE_USER"), true, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("Initiate payment auto-tokenizes raw card and publishes event")
    void testInitiatePayment() {
        PaymentRequest request = new PaymentRequest(
            CardType.CREDIT,
            "4532015112830366",
            null,
            "John Doe",
            "12",
            "2030",
            "123",
            new BigDecimal("100.00"),
            "USD",
            "Test payment",
            false
        );

        when(userServiceClient.getUserById(eq(1L), any())).thenReturn(Optional.of(Map.of("id", 1L)));
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction tx = invocation.getArgument(0);
            tx.setId("tx-uuid-1234");
            return tx;
        });

        PaymentResponse response = paymentService.initiatePayment(request, testUser, "Bearer token", "idemp-001");

        assertNotNull(response);
        assertEquals("tx-uuid-1234", response.transactionId());
        assertEquals(PaymentStatus.INITIATED, response.status());
        assertEquals("0366", response.cardLastFour());
        assertNotNull(response.cardToken());
        assertTrue(response.cardToken().startsWith("tok_visa_"));

        verify(kafkaProducer, times(1)).sendPaymentInitiated(any(PaymentEvent.class));
    }

    @Test
    @DisplayName("Idempotent request with existing key returns existing transaction without charging again")
    void testIdempotencyDeduplication() {
        PaymentRequest request = new PaymentRequest(
            CardType.DEBIT,
            "4532015112830366",
            null,
            "John Doe",
            "12",
            "2030",
            "123",
            new BigDecimal("50.00"),
            "USD",
            "Test payment",
            false
        );

        PaymentTransaction existing = new PaymentTransaction();
        existing.setId("tx-existing-999");
        existing.setUserId(1L);
        existing.setIdempotencyKey("idemp-duplicate");
        existing.setAmount(new BigDecimal("50.00"));
        existing.setCurrency("USD");
        existing.setStatus(PaymentStatus.COMPLETED);
        existing.setCardLastFour("0366");
        existing.setCardType(CardType.DEBIT);
        existing.setCardProvider(CardProvider.VISA);

        when(transactionRepository.findByUserIdAndIdempotencyKey(1L, "idemp-duplicate"))
            .thenReturn(Optional.of(existing));

        PaymentResponse response = paymentService.initiatePayment(request, testUser, "Bearer token", "idemp-duplicate");

        assertNotNull(response);
        assertEquals("tx-existing-999", response.transactionId());
        assertEquals(PaymentStatus.COMPLETED, response.status());
        verify(kafkaProducer, never()).sendPaymentInitiated(any());
    }

    @Test
    @DisplayName("Refund payment transitions COMPLETED transaction to REFUNDED and emits event")
    void testRefundPayment() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setId("tx-refund-1");
        tx.setUserId(1L);
        tx.setAmount(new BigDecimal("150.00"));
        tx.setCurrency("USD");
        tx.setStatus(PaymentStatus.COMPLETED);
        tx.setCardLastFour("0366");
        tx.setCardType(CardType.CREDIT);
        tx.setCardProvider(CardProvider.VISA);

        when(transactionRepository.findById("tx-refund-1")).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefundRequest refundRequest = new RefundRequest(new BigDecimal("75.00"), "Item returned");
        PaymentResponse response = paymentService.refundPayment("tx-refund-1", refundRequest, 1L);

        assertEquals(PaymentStatus.REFUNDED, response.status());
        assertEquals(new BigDecimal("75.00"), response.refundAmount());
        assertEquals("Item returned", response.refundReason());

        verify(kafkaProducer, times(1)).sendPaymentRefunded(any(PaymentEvent.class));
    }

    @Test
    @DisplayName("Capture payment transitions AUTHORIZED hold to COMPLETED")
    void testCapturePayment() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setId("tx-cap-1");
        tx.setUserId(1L);
        tx.setAmount(new BigDecimal("200.00"));
        tx.setCurrency("USD");
        tx.setStatus(PaymentStatus.AUTHORIZED);
        tx.setCardLastFour("0366");
        tx.setCardType(CardType.CREDIT);
        tx.setCardProvider(CardProvider.VISA);

        when(transactionRepository.findById("tx-cap-1")).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.capturePayment("tx-cap-1", 1L);

        assertEquals(PaymentStatus.COMPLETED, response.status());
    }

    @Test
    @DisplayName("Failed payment cannot be captured and returns its failure reason")
    void testCaptureFailedPayment() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setId("tx-failed-1");
        tx.setUserId(1L);
        tx.setStatus(PaymentStatus.FAILED);
        tx.setFailureReason("Insufficient funds");
        when(transactionRepository.findById("tx-failed-1")).thenReturn(Optional.of(tx));

        PaymentFailureException exception = assertThrows(
            PaymentFailureException.class,
            () -> paymentService.capturePayment("tx-failed-1", 1L)
        );

        assertEquals("Payment failed: Insufficient funds", exception.getMessage());
        verify(transactionRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("Lists only the authenticated user's payments with the requested status")
    void testGetMyPaymentsByStatus() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setId("tx-completed-1");
        tx.setUserId(1L);
        tx.setStatus(PaymentStatus.COMPLETED);
        when(transactionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, PaymentStatus.COMPLETED))
            .thenReturn(List.of(tx));

        List<PaymentResponse> payments = paymentService.getMyPayments(1L, PaymentStatus.COMPLETED);

        assertEquals(1, payments.size());
        assertEquals("tx-completed-1", payments.get(0).transactionId());
        assertEquals(PaymentStatus.COMPLETED, payments.get(0).status());
        verify(transactionRepository).findByUserIdAndStatusOrderByCreatedAtDesc(1L, PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Analytics query calculates success rate, decline rate, and volume accurately")
    void testAnalytics() {
        when(transactionRepository.countByUserId(1L)).thenReturn(10L);
        when(transactionRepository.countByUserIdAndStatus(1L, PaymentStatus.COMPLETED)).thenReturn(8L);
        when(transactionRepository.countByUserIdAndStatus(1L, PaymentStatus.FAILED)).thenReturn(2L);
        when(transactionRepository.countByUserIdAndStatus(1L, PaymentStatus.REFUNDED)).thenReturn(1L);
        when(transactionRepository.countByUserIdAndStatus(1L, PaymentStatus.AUTHORIZED)).thenReturn(0L);
        when(transactionRepository.sumAmountByUserIdAndStatus(1L, PaymentStatus.COMPLETED))
            .thenReturn(new BigDecimal("1250.00"));

        PaymentAnalyticsResponse analytics = paymentService.getAnalytics(1L);

        assertNotNull(analytics);
        assertEquals(10L, analytics.totalTransactions());
        assertEquals(8L, analytics.completedTransactions());
        assertEquals(2L, analytics.failedTransactions());
        assertEquals(80.0, analytics.successRatePercentage());
        assertEquals(20.0, analytics.declineRatePercentage());
        assertEquals(new BigDecimal("1250.00"), analytics.totalProcessedVolume());
    }
}
