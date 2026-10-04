package com.paymentgateway.payment.service;

import com.paymentgateway.payment.entity.PaymentTransaction;
import com.paymentgateway.payment.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Real-Time Fraud Detection & Risk Scoring Engine:
 * Analyzes transaction risk via velocity scoring, threshold deviations,
 * and automated behavioral rules.
 */
@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    public enum FraudDecision {
        APPROVE,
        FLAG_FOR_REVIEW,
        REJECT
    }

    public record FraudAssessment(
        int riskScore,
        FraudDecision decision,
        String reason
    ) {}

    private final PaymentTransactionRepository transactionRepository;

    public FraudDetectionService(PaymentTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Evaluates a payment request and returns a risk score (0-100) and recommendation.
     */
    public FraudAssessment evaluateRisk(Long userId, BigDecimal amount, String currency, String cardNumber) {
        int score = 5; // baseline low risk
        StringBuilder reasons = new StringBuilder();

        // 1. Velocity check: number of transactions in the last 2 minutes
        Instant twoMinutesAgo = Instant.now().minus(2, ChronoUnit.MINUTES);
        long recentTxCount = transactionRepository.countByUserIdAndCreatedAtAfter(userId, twoMinutesAgo);

        if (recentTxCount >= 5) {
            score += 50;
            reasons.append("High velocity detected (").append(recentTxCount).append(" txs in 2 mins); ");
        } else if (recentTxCount >= 3) {
            score += 25;
            reasons.append("Moderate velocity detected; ");
        }

        // 2. High-value anomaly check
        if (amount.compareTo(new BigDecimal("25000.00")) > 0) {
            score += 45;
            reasons.append("Very high transaction amount (>25000 ").append(currency).append("); ");
        } else if (amount.compareTo(new BigDecimal("10000.00")) > 0) {
            score += 20;
            reasons.append("Elevated transaction amount (>10000 ").append(currency).append("); ");
        }

        // 3. Test card pattern check
        if (cardNumber != null && cardNumber.startsWith("400000000000")) {
            score += 30;
            reasons.append("Simulated test card pattern flagged; ");
        }

        int finalScore = Math.min(score, 100);
        FraudDecision decision;
        if (finalScore >= 80) {
            decision = FraudDecision.REJECT;
        } else if (finalScore >= 50) {
            decision = FraudDecision.FLAG_FOR_REVIEW;
        } else {
            decision = FraudDecision.APPROVE;
        }

        String summaryReason = reasons.length() > 0 ? reasons.toString().trim() : "Low risk assessment";
        log.info("Fraud assessment for user {} (amount: {} {}): Score={}, Decision={}, Reason={}",
            userId, amount, currency, finalScore, decision, summaryReason);

        return new FraudAssessment(finalScore, decision, summaryReason);
    }
}
