package com.paymentgateway.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.paymentgateway.payment.repository.PaymentTransactionRepository;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;

    private FraudDetectionService fraudService;

    @BeforeEach
    void setUp() {
        fraudService = new FraudDetectionService(transactionRepository);
    }

    @Test
    @DisplayName("Normal transaction with low velocity should be APPROVED")
    void testNormalTransactionApproved() {
        when(transactionRepository.countByUserIdAndCreatedAtAfter(eq(1L), any(Instant.class)))
            .thenReturn(0L);

        FraudDetectionService.FraudAssessment assessment = fraudService.evaluateRisk(
            1L, new BigDecimal("150.00"), "USD", "4532015112830366"
        );

        assertEquals(FraudDetectionService.FraudDecision.APPROVE, assessment.decision());
        assertTrue(assessment.riskScore() < 50);
    }

    @Test
    @DisplayName("High velocity transaction barrage should increase risk score")
    void testHighVelocityFlagged() {
        when(transactionRepository.countByUserIdAndCreatedAtAfter(eq(1L), any(Instant.class)))
            .thenReturn(6L);

        FraudDetectionService.FraudAssessment assessment = fraudService.evaluateRisk(
            1L, new BigDecimal("200.00"), "USD", "4532015112830366"
        );

        assertTrue(assessment.riskScore() >= 50);
        assertTrue(assessment.reason().contains("High velocity"));
    }

    @Test
    @DisplayName("Extremely high amount combined with velocity should be REJECTED")
    void testExtremeAmountAndVelocityRejected() {
        when(transactionRepository.countByUserIdAndCreatedAtAfter(eq(1L), any(Instant.class)))
            .thenReturn(6L);

        FraudDetectionService.FraudAssessment assessment = fraudService.evaluateRisk(
            1L, new BigDecimal("35000.00"), "USD", "4532015112830366"
        );

        assertEquals(FraudDetectionService.FraudDecision.REJECT, assessment.decision());
        assertTrue(assessment.riskScore() >= 80);
    }
}
