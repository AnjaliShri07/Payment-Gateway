package com.paymentgateway.payment.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.paymentgateway.payment.enums.CardProvider;
import com.paymentgateway.payment.enums.CardType;
import com.paymentgateway.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Kafka event published across payment processing topics:
 * - payment.initiated
 * - payment.completed
 * - payment.failed
 * - payment.refunded
 */
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class PaymentEvent {

    private String transactionId;
    private Long userId;
    private String userEmail;
    private String idempotencyKey;
    private String cardHolderName;
    private String cardLastFour;
    private String cardToken;
    private CardType cardType;
    private CardProvider cardProvider;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String authCode;
    private String acquirerReferenceNumber;
    private Integer riskScore;
    private String description;
    private String failureReason;
    private BigDecimal refundAmount;
    private String refundReason;
    private Boolean threeDSecureRequired;
    private String rawCardNumber; // Transient in event for processor; cleared upon completion
    private String expiryMonth;
    private String expiryYear;
    private String cvv;
    private Instant timestamp;
    private String username;
    private List<String> roles;
    private Boolean userEnabled;
    private Instant userCreatedAt;
    private Instant userUpdatedAt;

    public PaymentEvent() {
        this.timestamp = Instant.now();
    }

    @JsonProperty("payload")
    public PaymentPayload getPayload() {
        return new PaymentPayload(userId, username, userEmail, roles, userEnabled, userCreatedAt, userUpdatedAt);
    }

    @JsonProperty("payload")
    public void setPayload(PaymentPayload payload) {
        if (payload == null) {
            return;
        }
        userId = payload.userId();
        username = payload.username();
        userEmail = payload.email();
        roles = payload.roles();
        userEnabled = payload.enabled();
        userCreatedAt = payload.createdAt();
        userUpdatedAt = payload.updatedAt();
    }

    @JsonProperty("payment")
    public PaymentDetails getPayment() {
        PaymentDetails details = new PaymentDetails();
        details.setTransactionId(transactionId);
        details.setIdempotencyKey(idempotencyKey);
        details.setCardHolderName(cardHolderName);
        details.setCardLastFour(cardLastFour);
        details.setCardToken(cardToken);
        details.setCardType(cardType);
        details.setCardProvider(cardProvider);
        details.setAmount(amount);
        details.setCurrency(currency);
        details.setStatus(status);
        details.setAuthCode(authCode);
        details.setAcquirerReferenceNumber(acquirerReferenceNumber);
        details.setRiskScore(riskScore);
        details.setDescription(description);
        details.setFailureReason(failureReason);
        details.setRefundAmount(refundAmount);
        details.setRefundReason(refundReason);
        details.setThreeDSecureRequired(threeDSecureRequired);
        details.setRawCardNumber(rawCardNumber);
        details.setExpiryMonth(expiryMonth);
        details.setExpiryYear(expiryYear);
        details.setCvv(cvv);
        return details;
    }

    @JsonProperty("payment")
    public void setPayment(PaymentDetails details) {
        if (details == null) {
            return;
        }
        transactionId = details.getTransactionId();
        idempotencyKey = details.getIdempotencyKey();
        cardHolderName = details.getCardHolderName();
        cardLastFour = details.getCardLastFour();
        cardToken = details.getCardToken();
        cardType = details.getCardType();
        cardProvider = details.getCardProvider();
        amount = details.getAmount();
        currency = details.getCurrency();
        status = details.getStatus();
        authCode = details.getAuthCode();
        acquirerReferenceNumber = details.getAcquirerReferenceNumber();
        riskScore = details.getRiskScore();
        description = details.getDescription();
        failureReason = details.getFailureReason();
        refundAmount = details.getRefundAmount();
        refundReason = details.getRefundReason();
        threeDSecureRequired = details.getThreeDSecureRequired();
        rawCardNumber = details.getRawCardNumber();
        expiryMonth = details.getExpiryMonth();
        expiryYear = details.getExpiryYear();
        cvv = details.getCvv();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCardLastFour() {
        return cardLastFour;
    }

    public void setCardLastFour(String cardLastFour) {
        this.cardLastFour = cardLastFour;
    }

    public String getCardToken() {
        return cardToken;
    }

    public void setCardToken(String cardToken) {
        this.cardToken = cardToken;
    }

    public CardType getCardType() {
        return cardType;
    }

    public void setCardType(CardType cardType) {
        this.cardType = cardType;
    }

    public CardProvider getCardProvider() {
        return cardProvider;
    }

    public void setCardProvider(CardProvider cardProvider) {
        this.cardProvider = cardProvider;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getAcquirerReferenceNumber() {
        return acquirerReferenceNumber;
    }

    public void setAcquirerReferenceNumber(String acquirerReferenceNumber) {
        this.acquirerReferenceNumber = acquirerReferenceNumber;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public Boolean getThreeDSecureRequired() {
        return threeDSecureRequired;
    }

    public void setThreeDSecureRequired(Boolean threeDSecureRequired) {
        this.threeDSecureRequired = threeDSecureRequired;
    }

    public String getRawCardNumber() {
        return rawCardNumber;
    }

    public void setRawCardNumber(String rawCardNumber) {
        this.rawCardNumber = rawCardNumber;
    }

    public String getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(String expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public String getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(String expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    @JsonProperty("timestamp")
    public Instant getTimestamp() {
        return timestamp;
    }

    @JsonProperty("timestamp")
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
