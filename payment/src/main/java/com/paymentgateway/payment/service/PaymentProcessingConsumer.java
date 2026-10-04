package com.paymentgateway.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.payment.document.PaymentAuditLog;
import com.paymentgateway.payment.entity.PaymentTransaction;
import com.paymentgateway.payment.enums.CardProvider;
import com.paymentgateway.payment.enums.CardType;
import com.paymentgateway.payment.enums.PaymentStatus;
import com.paymentgateway.payment.kafka.PaymentKafkaProducer;
import com.paymentgateway.payment.model.PaymentEvent;
import com.paymentgateway.payment.model.PaymentPayload;
import com.paymentgateway.payment.processor.CardPaymentProcessor;
import com.paymentgateway.payment.processor.PaymentProcessingResult;
import com.paymentgateway.payment.repository.PaymentAuditLogRepository;
import com.paymentgateway.payment.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Consumes payment.initiated events from Kafka, performs card validations (Luhn, CVV, expiry),
 * evaluates real-time fraud risk score, delegates to CNP DebitCardProcessor or CreditCardProcessor,
 * updates MySQL and MongoDB, and publishes payment.completed or payment.failed to Kafka.
 */
@Service
public class PaymentProcessingConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessingConsumer.class);

    private final ObjectMapper objectMapper;
    private final CardValidationService cardValidationService;
    private final FraudDetectionService fraudDetectionService;
    private final Map<CardType, CardPaymentProcessor> processors;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentAuditLogRepository auditLogRepository;
    private final PaymentKafkaProducer kafkaProducer;

    public PaymentProcessingConsumer(
        ObjectMapper objectMapper,
        CardValidationService cardValidationService,
        FraudDetectionService fraudDetectionService,
        List<CardPaymentProcessor> processorList,
        PaymentTransactionRepository transactionRepository,
        PaymentAuditLogRepository auditLogRepository,
        PaymentKafkaProducer kafkaProducer
    ) {
        this.objectMapper = objectMapper;
        this.cardValidationService = cardValidationService;
        this.fraudDetectionService = fraudDetectionService;
        this.processors = processorList.stream()
            .collect(Collectors.toMap(CardPaymentProcessor::getSupportedCardType, p -> p));
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.kafkaProducer = kafkaProducer;
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-initiated:payment.initiated}", groupId = "${spring.kafka.consumer.group-id:payment-service-group}")
    @Transactional
    public void consumePaymentInitiated(String message) {
        log.info("Received Kafka event on topic [payment.initiated]");

        PaymentEvent event;
        try {
            event = objectMapper.readValue(message, PaymentEvent.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize Kafka event: {}", ex.getMessage());
            return;
        }

        String txId = event.getTransactionId();
        Optional<PaymentTransaction> txOpt = transactionRepository.findById(txId);
        if (txOpt.isEmpty()) {
            log.error("Transaction not found in MySQL for ID: {}", txId);
            return;
        }

        PaymentTransaction tx = txOpt.get();
        tx.setStatus(PaymentStatus.PROCESSING);
        transactionRepository.save(tx);
        recordAuditLog(tx, "Payment marked as PROCESSING in consumer", event.getPayload());

        // 1. Validate Luhn algorithm (if raw card number provided)
        if (event.getRawCardNumber() != null && !cardValidationService.isValidLuhn(event.getRawCardNumber())) {
            failPayment(tx, event, "Invalid card number (Luhn check failed)");
            return;
        }

        // 2. Validate expiration date
        if (event.getExpiryMonth() != null && event.getExpiryYear() != null
            && !cardValidationService.isNotExpired(event.getExpiryMonth(), event.getExpiryYear())) {
            failPayment(tx, event, "Card has expired");
            return;
        }

        // 3. Validate CVV
        if (event.getCvv() != null) {
            CardProvider detectedProvider = event.getCardProvider() != null
                ? event.getCardProvider()
                : cardValidationService.detectCardProvider(event.getRawCardNumber());
            if (!cardValidationService.isValidCvv(event.getCvv(), detectedProvider)) {
                failPayment(tx, event, "Invalid CVV for " + detectedProvider);
                return;
            }
        }

        // 4. Real-Time Fraud Risk Scoring
        FraudDetectionService.FraudAssessment assessment = fraudDetectionService.evaluateRisk(
            tx.getUserId(), tx.getAmount(), tx.getCurrency(), event.getRawCardNumber()
        );
        tx.setRiskScore(assessment.riskScore());
        event.setRiskScore(assessment.riskScore());

        if (assessment.decision() == FraudDetectionService.FraudDecision.REJECT) {
            failPayment(tx, event, "Transaction declined by automated Fraud Detection: " + assessment.reason());
            return;
        }

        recordAuditLog(
            tx,
            "Fraud check passed (Risk Score: " + assessment.riskScore() + " | " + assessment.decision() + ")",
            event.getPayload()
        );

        // 5. Delegate to CNP DebitCardProcessor or CreditCardProcessor
        CardPaymentProcessor processor = processors.get(event.getCardType());
        if (processor == null) {
            failPayment(tx, event, "Unsupported card type: " + event.getCardType());
            return;
        }

        PaymentProcessingResult result = processor.process(event);

        if (result.success()) {
            completePayment(tx, event, result);
        } else {
            failPayment(tx, event, result.failureReason());
        }
    }

    private void completePayment(PaymentTransaction tx, PaymentEvent event, PaymentProcessingResult result) {
        PaymentStatus finalStatus = result.resultingStatus() != null ? result.resultingStatus() : PaymentStatus.COMPLETED;
        tx.setStatus(finalStatus);
        tx.setAuthCode(result.authCode());
        tx.setAcquirerReferenceNumber(result.acquirerReferenceNumber());
        tx.setProcessedAt(Instant.now());
        tx.setDescription((tx.getDescription() != null ? tx.getDescription() + " | " : "")
            + "AuthCode: " + result.authCode() + " | ARN: " + result.acquirerReferenceNumber());
        transactionRepository.save(tx);

        event.setStatus(finalStatus);
        event.setAuthCode(result.authCode());
        event.setAcquirerReferenceNumber(result.acquirerReferenceNumber());
        event.setRawCardNumber(null);
        event.setCvv(null);

        recordAuditLog(tx, "Payment successfully " + finalStatus + ". AuthCode: " + result.authCode()
            + ", ARN: " + result.acquirerReferenceNumber(), event.getPayload());
        kafkaProducer.sendPaymentCompleted(event);

        log.info("Transaction [{}] successfully completed via {} processor (AuthCode: {}, ARN: {})",
            tx.getId(), tx.getCardType(), result.authCode(), result.acquirerReferenceNumber());
    }

    private void failPayment(PaymentTransaction tx, PaymentEvent event, String reason) {
        tx.setStatus(PaymentStatus.FAILED);
        tx.setFailureReason(reason);
        tx.setProcessedAt(Instant.now());
        transactionRepository.save(tx);

        event.setStatus(PaymentStatus.FAILED);
        event.setFailureReason(reason);
        event.setRawCardNumber(null);
        event.setCvv(null);

        recordAuditLog(tx, "Payment FAILED. Reason: " + reason, event.getPayload());
        kafkaProducer.sendPaymentFailed(event);

        log.warn("Transaction [{}] FAILED: {}", tx.getId(), reason);
    }

    private void recordAuditLog(PaymentTransaction tx, String note, PaymentPayload payload) {
        try {
            PaymentAuditLog auditLog = new PaymentAuditLog();
            auditLog.setTransactionId(tx.getId());
            auditLog.setUserId(tx.getUserId());
            if (payload != null) {
                auditLog.setPayload(payload);
            }
            auditLog.setIdempotencyKey(tx.getIdempotencyKey());
            auditLog.setStatus(tx.getStatus());
            auditLog.setAmount(tx.getAmount());
            auditLog.setCurrency(tx.getCurrency());
            auditLog.setRefundAmount(tx.getRefundAmount());
            auditLog.setRefundReason(tx.getRefundReason());
            auditLog.setCardHolderName(tx.getCardHolderName());
            auditLog.setCardLastFour(tx.getCardLastFour());
            auditLog.setCardToken(tx.getCardToken());
            auditLog.setCardType(tx.getCardType() != null ? tx.getCardType().name() : null);
            auditLog.setCardProvider(tx.getCardProvider() != null ? tx.getCardProvider().name() : null);
            auditLog.setAuthCode(tx.getAuthCode());
            auditLog.setAcquirerReferenceNumber(tx.getAcquirerReferenceNumber());
            auditLog.setRiskScore(tx.getRiskScore());
            auditLog.setDescription(note);
            auditLog.setFailureReason(tx.getFailureReason());
            auditLog.setEventTime(Instant.now());

            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Could not persist audit log to MongoDB: {}", ex.getMessage());
        }
    }
}
