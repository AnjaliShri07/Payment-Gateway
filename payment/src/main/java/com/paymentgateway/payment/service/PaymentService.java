package com.paymentgateway.payment.service;

import com.paymentgateway.payment.client.UserServiceClient;
import com.paymentgateway.payment.client.dto.AuthUserProfile;
import com.paymentgateway.payment.document.PaymentAuditLog;
import com.paymentgateway.payment.dto.request.PaymentRequest;
import com.paymentgateway.payment.dto.request.RefundRequest;
import com.paymentgateway.payment.dto.request.TokenizeCardRequest;
import com.paymentgateway.payment.dto.response.PaymentAnalyticsResponse;
import com.paymentgateway.payment.dto.response.PaymentResponse;
import com.paymentgateway.payment.dto.response.TokenResponse;
import com.paymentgateway.payment.entity.PaymentTransaction;
import com.paymentgateway.payment.enums.CardProvider;
import com.paymentgateway.payment.enums.CardType;
import com.paymentgateway.payment.enums.PaymentStatus;
import com.paymentgateway.payment.exception.PaymentFailureException;
import com.paymentgateway.payment.kafka.PaymentKafkaProducer;
import com.paymentgateway.payment.model.PaymentEvent;
import com.paymentgateway.payment.model.PaymentPayload;
import com.paymentgateway.payment.repository.PaymentAuditLogRepository;
import com.paymentgateway.payment.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentAuditLogRepository auditLogRepository;
    private final PaymentKafkaProducer kafkaProducer;
    private final CardValidationService cardValidationService;
    private final TokenizationService tokenizationService;
    private final CurrencyService currencyService;
    private final UserServiceClient userServiceClient;

    public PaymentService(
        PaymentTransactionRepository transactionRepository,
        PaymentAuditLogRepository auditLogRepository,
        PaymentKafkaProducer kafkaProducer,
        CardValidationService cardValidationService,
        TokenizationService tokenizationService,
        CurrencyService currencyService,
        UserServiceClient userServiceClient
    ) {
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.kafkaProducer = kafkaProducer;
        this.cardValidationService = cardValidationService;
        this.tokenizationService = tokenizationService;
        this.currencyService = currencyService;
        this.userServiceClient = userServiceClient;
    }

    private PaymentPayload toPayload(AuthUserProfile user) {
        return new PaymentPayload(
            user.id(),
            user.username(),
            user.email(),
            user.roles(),
            user.enabled(),
            user.createdAt(),
            user.updatedAt()
        );
    }

    /**
     * Initiates a card payment for the authenticated user.
     * Features:
     * - Idempotency deduplication using Idempotency-Key.
     * - Automatic PCI-DSS tokenization (or reuse of existing cardToken).
     * - Multi-currency ISO code validation.
     * - Verification against User microservice.
     * - Dual persistence in MySQL and MongoDB.
     * - Asynchronous publishing to Kafka payment.initiated topic.
     */
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request, AuthUserProfile authenticatedUser,
                                           String bearerToken, String idempotencyKey) {

        // 1. Idempotency Check: Prevent duplicate charges for the same key
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<PaymentTransaction> existingTx = transactionRepository
                .findByUserIdAndIdempotencyKey(authenticatedUser.id(), idempotencyKey.trim());
            if (existingTx.isPresent()) {
                log.info("Idempotent request detected for key [{}]. Returning existing transaction [{}]",
                    idempotencyKey, existingTx.get().getId());
                return mapToResponse(existingTx.get());
            }
        }

        // 2. Validate Currency
        String currency = request.currency().toUpperCase().trim();
        if (!currencyService.isSupportedCurrency(currency)) {
            throw new IllegalArgumentException("Unsupported currency: " + currency
                + ". Supported currencies are: " + currencyService.getSupportedCurrencies());
        }

        log.info("Initiating {} payment for user {} (ID: {}) for amount: {} {}",
            request.cardType(), authenticatedUser.username(), authenticatedUser.id(),
            request.amount(), currency);

        // 3. User Service profile verification
        Optional<Map<String, Object>> userObj = userServiceClient.getUserById(authenticatedUser.id(), bearerToken);
        if (userObj.isPresent()) {
            log.info("Verified active user profile against User microservice for user ID: {}", authenticatedUser.id());
        }

        // 4. Tokenization & Card Details Resolution
        String cardToken = request.cardToken();
        String cleanCardNumber = null;
        String lastFour;
        CardProvider provider;
        CardType resolvedCardType = request.cardType();
        String cardHolder = request.cardHolderName();
        String expiryMonth = request.expiryMonth();
        String expiryYear = request.expiryYear();
        String cvv = request.cvv();

        if (cardToken != null && !cardToken.trim().isEmpty()) {
            // Using existing token from vault
            Optional<TokenizationService.VaultedCard> vaultedOpt = tokenizationService.getVaultedCard(cardToken.trim());
            if (vaultedOpt.isEmpty()) {
                throw new IllegalArgumentException("Invalid or unrecognized cardToken: " + cardToken);
            }
            TokenizationService.VaultedCard vaulted = vaultedOpt.get();
            cleanCardNumber = vaulted.rawCardNumber();
            lastFour = vaulted.cardLastFour();
            provider = vaulted.cardProvider();
            resolvedCardType = vaulted.cardType() != null ? vaulted.cardType() : request.cardType();
            cardHolder = vaulted.cardHolderName();
            expiryMonth = vaulted.expiryMonth();
            expiryYear = vaulted.expiryYear();
        } else if (request.cardNumber() != null && !request.cardNumber().trim().isEmpty()) {
            // Auto-tokenize raw card data for PCI-DSS compliance
            TokenizeCardRequest tokenReq = new TokenizeCardRequest(
                request.cardType(),
                request.cardNumber(),
                request.cardHolderName(),
                request.expiryMonth(),
                request.expiryYear(),
                request.cvv()
            );
            TokenResponse tokenResp = tokenizationService.tokenize(tokenReq);
            cardToken = tokenResp.cardToken();
            lastFour = tokenResp.cardLastFour();
            provider = tokenResp.cardProvider();
            cleanCardNumber = request.cardNumber().replaceAll("\\s+", "");
        } else {
            throw new IllegalArgumentException("Either cardNumber or cardToken must be provided");
        }

        // 5. Persist initial transaction state to MySQL
        PaymentTransaction tx = new PaymentTransaction();
        tx.setUserId(authenticatedUser.id());
        tx.setIdempotencyKey(idempotencyKey != null ? idempotencyKey.trim() : null);
        tx.setCardHolderName(cardHolder != null ? cardHolder : "Cardholder");
        tx.setCardLastFour(lastFour);
        tx.setCardToken(cardToken);
        tx.setCardType(resolvedCardType);
        tx.setCardProvider(provider);
        tx.setAmount(request.amount());
        tx.setCurrency(currency);
        tx.setStatus(PaymentStatus.INITIATED);
        tx.setDescription(request.description());

        PaymentTransaction savedTx = transactionRepository.save(tx);

        // 6. Log initial audit entry to MongoDB
        recordAuditLog(savedTx, "Payment INITIATED by user " + authenticatedUser.username()
            + " (Token: " + cardToken + ")", toPayload(authenticatedUser));

        // 7. Publish to Kafka topic: payment.initiated
        PaymentEvent event = new PaymentEvent();
        event.setTransactionId(savedTx.getId());
        event.setUserId(authenticatedUser.id());
        event.setUserEmail(authenticatedUser.email());
        event.setPayload(toPayload(authenticatedUser));
        event.setIdempotencyKey(savedTx.getIdempotencyKey());
        event.setCardHolderName(savedTx.getCardHolderName());
        event.setCardLastFour(lastFour);
        event.setCardToken(cardToken);
        event.setCardType(savedTx.getCardType());
        event.setCardProvider(savedTx.getCardProvider());
        event.setAmount(savedTx.getAmount());
        event.setCurrency(savedTx.getCurrency());
        event.setStatus(PaymentStatus.INITIATED);
        event.setDescription(savedTx.getDescription());
        event.setThreeDSecureRequired(request.threeDSecureRequired());
        event.setRawCardNumber(cleanCardNumber);
        event.setExpiryMonth(expiryMonth);
        event.setExpiryYear(expiryYear);
        event.setCvv(cvv);

        kafkaProducer.sendPaymentInitiated(event);

        return mapToResponse(savedTx);
    }

    /**
     * Standalone card tokenization endpoint.
     */
    public TokenResponse tokenizeCard(TokenizeCardRequest request) {
        return tokenizationService.tokenize(request);
    }

    /**
     * Process a refund for a previously completed/authorized payment transaction.
     */
    @Transactional
    public PaymentResponse refundPayment(String transactionId, RefundRequest request, Long userId) {
        return refundPayment(transactionId, request, userId, null);
    }

    @Transactional
    public PaymentResponse refundPayment(String transactionId, RefundRequest request, AuthUserProfile user) {
        return refundPayment(transactionId, request, user.id(), toPayload(user));
    }

    private PaymentResponse refundPayment(
        String transactionId,
        RefundRequest request,
        Long userId,
        PaymentPayload payload
    ) {
        PaymentTransaction tx = transactionRepository.findById(transactionId)
            .filter(t -> t.getUserId().equals(userId))
            .orElseThrow(() -> new NoSuchElementException("Transaction not found for ID: " + transactionId));

        if (tx.getStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Transaction is already refunded.");
        }
        if (tx.getStatus() == PaymentStatus.FAILED) {
            throw new PaymentFailureException(paymentFailureMessage(tx));
        }
        if (tx.getStatus() != PaymentStatus.COMPLETED && tx.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Only COMPLETED or AUTHORIZED transactions can be refunded. Current status: " + tx.getStatus());
        }

        BigDecimal refundAmt = (request != null && request.amount() != null) ? request.amount() : tx.getAmount();
        if (refundAmt.compareTo(tx.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount (" + refundAmt + ") cannot exceed original charge (" + tx.getAmount() + ")");
        }

        String reason = (request != null && request.reason() != null && !request.reason().trim().isEmpty())
            ? request.reason().trim()
            : "Customer requested refund";

        tx.setStatus(PaymentStatus.REFUNDED);
        tx.setRefundAmount(refundAmt);
        tx.setRefundReason(reason);
        tx.setRefundedAt(Instant.now());
        PaymentTransaction updated = transactionRepository.save(tx);

        recordAuditLog(
            updated,
            "Payment REFUNDED. Amount: " + refundAmt + " " + tx.getCurrency() + " | Reason: " + reason,
            payload
        );

        // Publish to Kafka: payment.refunded
        PaymentEvent event = new PaymentEvent();
        event.setTransactionId(updated.getId());
        event.setUserId(updated.getUserId());
        event.setPayload(payload != null
            ? payload
            : new PaymentPayload(updated.getUserId(), null, null, null, false, null, null));
        event.setCardLastFour(updated.getCardLastFour());
        event.setCardType(updated.getCardType());
        event.setCardProvider(updated.getCardProvider());
        event.setAmount(updated.getAmount());
        event.setRefundAmount(refundAmt);
        event.setRefundReason(reason);
        event.setCurrency(updated.getCurrency());
        event.setStatus(PaymentStatus.REFUNDED);
        kafkaProducer.sendPaymentRefunded(event);

        log.info("Refund processed for transaction [{}] for amount {} {}",
            transactionId, refundAmt, updated.getCurrency());

        return mapToResponse(updated);
    }

    /**
     * Manual capture endpoint for authorized transactions.
     */
    @Transactional
    public PaymentResponse capturePayment(String transactionId, Long userId) {
        return capturePayment(transactionId, userId, null);
    }

    @Transactional
    public PaymentResponse capturePayment(String transactionId, AuthUserProfile user) {
        return capturePayment(transactionId, user.id(), toPayload(user));
    }

    private PaymentResponse capturePayment(String transactionId, Long userId, PaymentPayload payload) {
        PaymentTransaction tx = transactionRepository.findById(transactionId)
            .filter(t -> t.getUserId().equals(userId))
            .orElseThrow(() -> new NoSuchElementException("Transaction not found for ID: " + transactionId));

        if (tx.getStatus() == PaymentStatus.FAILED) {
            throw new PaymentFailureException(paymentFailureMessage(tx));
        }
        if (tx.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Only AUTHORIZED transactions can be captured. Current status: " + tx.getStatus());
        }

        tx.setStatus(PaymentStatus.COMPLETED);
        tx.setProcessedAt(Instant.now());
        PaymentTransaction saved = transactionRepository.save(tx);

        recordAuditLog(saved, "Payment CAPTURED and marked COMPLETED", payload);
        return mapToResponse(saved);
    }

    /**
     * Provides aggregated operational metrics and analytics for transactions.
     */
    public PaymentAnalyticsResponse getAnalytics(Long userId) {
        long total = userId != null ? transactionRepository.countByUserId(userId) : transactionRepository.count();
        long completed = userId != null
            ? transactionRepository.countByUserIdAndStatus(userId, PaymentStatus.COMPLETED)
            : transactionRepository.countByStatus(PaymentStatus.COMPLETED);
        long failed = userId != null
            ? transactionRepository.countByUserIdAndStatus(userId, PaymentStatus.FAILED)
            : transactionRepository.countByStatus(PaymentStatus.FAILED);
        long refunded = userId != null
            ? transactionRepository.countByUserIdAndStatus(userId, PaymentStatus.REFUNDED)
            : transactionRepository.countByStatus(PaymentStatus.REFUNDED);
        long authorized = userId != null
            ? transactionRepository.countByUserIdAndStatus(userId, PaymentStatus.AUTHORIZED)
            : transactionRepository.countByStatus(PaymentStatus.AUTHORIZED);

        double successRate = total > 0 ? ((double) completed / total) * 100.0 : 0.0;
        double declineRate = total > 0 ? ((double) failed / total) * 100.0 : 0.0;

        BigDecimal volume = userId != null
            ? transactionRepository.sumAmountByUserIdAndStatus(userId, PaymentStatus.COMPLETED)
            : transactionRepository.sumAmountByStatus(PaymentStatus.COMPLETED);
        if (volume == null) {
            volume = BigDecimal.ZERO;
        }

        Map<PaymentStatus, Long> statusBreakdown = new EnumMap<>(PaymentStatus.class);
        for (PaymentStatus s : PaymentStatus.values()) {
            long c = userId != null
                ? transactionRepository.countByUserIdAndStatus(userId, s)
                : transactionRepository.countByStatus(s);
            statusBreakdown.put(s, c);
        }

        return new PaymentAnalyticsResponse(
            total,
            completed,
            failed,
            refunded,
            authorized,
            BigDecimal.valueOf(successRate).setScale(2, RoundingMode.HALF_UP).doubleValue(),
            BigDecimal.valueOf(declineRate).setScale(2, RoundingMode.HALF_UP).doubleValue(),
            volume,
            "USD",
            statusBreakdown
        );
    }

    public Optional<PaymentResponse> getPaymentById(String transactionId, Long userId) {
        return transactionRepository.findById(transactionId)
            .filter(tx -> tx.getUserId().equals(userId))
            .map(this::mapToResponse);
    }

    public List<PaymentResponse> getMyPayments(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public List<PaymentResponse> getMyPayments(Long userId, PaymentStatus status) {
        return transactionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    private String paymentFailureMessage(PaymentTransaction tx) {
        String reason = tx.getFailureReason();
        return reason == null || reason.isBlank()
            ? "Payment failed and cannot be processed further."
            : "Payment failed: " + reason;
    }

    public List<PaymentAuditLog> getPaymentAuditLogs(String transactionId) {
        return auditLogRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId);
    }

    private void recordAuditLog(PaymentTransaction tx, String note) {
        recordAuditLog(tx, note, null);
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
            auditLog.setCardLastFour(tx.getCardLastFour());
            auditLog.setCardToken(tx.getCardToken());
            auditLog.setCardHolderName(tx.getCardHolderName());
            auditLog.setCardType(tx.getCardType() != null ? tx.getCardType().name() : null);
            auditLog.setCardProvider(tx.getCardProvider() != null ? tx.getCardProvider().name() : null);
            auditLog.setAuthCode(tx.getAuthCode());
            auditLog.setAcquirerReferenceNumber(tx.getAcquirerReferenceNumber());
            auditLog.setRiskScore(tx.getRiskScore());
            auditLog.setDescription(note);
            auditLog.setEventTime(Instant.now());

            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Could not record audit log in MongoDB: {}", ex.getMessage());
        }
    }

    public PaymentResponse mapToResponse(PaymentTransaction tx) {
        return new PaymentResponse(
            tx.getId(),
            tx.getUserId(),
            tx.getIdempotencyKey(),
            tx.getCardHolderName(),
            tx.getCardLastFour(),
            tx.getCardToken(),
            tx.getCardType(),
            tx.getCardProvider(),
            tx.getAmount(),
            tx.getCurrency(),
            tx.getStatus(),
            tx.getAuthCode(),
            tx.getAcquirerReferenceNumber(),
            tx.getRiskScore(),
            tx.getDescription(),
            tx.getFailureReason(),
            tx.getRefundAmount(),
            tx.getRefundReason(),
            tx.getCreatedAt(),
            tx.getProcessedAt(),
            tx.getRefundedAt()
        );
    }
}
