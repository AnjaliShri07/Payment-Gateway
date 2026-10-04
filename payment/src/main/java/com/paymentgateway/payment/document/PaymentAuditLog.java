package com.paymentgateway.payment.document;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.paymentgateway.payment.enums.PaymentStatus;
import com.paymentgateway.payment.model.PaymentDetails;
import com.paymentgateway.payment.model.PaymentPayload;

import java.math.BigDecimal;
import java.time.Instant;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class PaymentAuditLog {

    private String id;

    private String transactionId;

    private Long userId;

    private String idempotencyKey;

    private PaymentStatus status;

    private BigDecimal amount;

    private String currency;

    private String cardLastFour;

    private String cardToken;

    private String cardType;

    private String cardProvider;

    private String authCode;

    private String acquirerReferenceNumber;

    private Integer riskScore;

    private String description;

    private String failureReason;

    private Instant eventTime;

    private Instant createdAt;

    private String username;
    private String email;
    private java.util.List<String> roles;
    private Boolean userEnabled;
    private Instant userCreatedAt;
    private Instant userUpdatedAt;
    private String cardHolderName;
    private BigDecimal refundAmount;
    private String refundReason;

    public PaymentAuditLog() {
        this.createdAt = Instant.now();
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("payload")
    public PaymentPayload getPayload() {
        return new PaymentPayload(userId, username, email, roles, userEnabled, userCreatedAt, userUpdatedAt);
    }

    @JsonProperty("payload")
    public void setPayload(PaymentPayload payload) {
        if (payload == null) {
            return;
        }
        userId = payload.userId();
        username = payload.username();
        email = payload.email();
        roles = payload.roles();
        userEnabled = payload.enabled();
        userCreatedAt = payload.createdAt();
        userUpdatedAt = payload.updatedAt();
    }

    @JsonProperty("payment")
    public PaymentDetails getPayment() {
        PaymentDetails payment = new PaymentDetails();
        payment.setTransactionId(transactionId);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setCardHolderName(cardHolderName);
        payment.setCardLastFour(cardLastFour);
        payment.setCardToken(cardToken);
        if (cardType != null) {
            payment.setCardType(com.paymentgateway.payment.enums.CardType.valueOf(cardType));
        }
        if (cardProvider != null) {
            payment.setCardProvider(com.paymentgateway.payment.enums.CardProvider.valueOf(cardProvider));
        }
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(status);
        payment.setAuthCode(authCode);
        payment.setAcquirerReferenceNumber(acquirerReferenceNumber);
        payment.setRiskScore(riskScore);
        payment.setDescription(description);
        payment.setFailureReason(failureReason);
        payment.setRefundAmount(refundAmount);
        payment.setRefundReason(refundReason);
        return payment;
    }

    @JsonProperty("payment")
    public void setPayment(PaymentDetails payment) {
        if (payment == null) {
            return;
        }
        transactionId = payment.getTransactionId();
        idempotencyKey = payment.getIdempotencyKey();
        cardHolderName = payment.getCardHolderName();
        cardLastFour = payment.getCardLastFour();
        cardToken = payment.getCardToken();
        cardType = payment.getCardType() != null ? payment.getCardType().name() : null;
        cardProvider = payment.getCardProvider() != null ? payment.getCardProvider().name() : null;
        amount = payment.getAmount();
        currency = payment.getCurrency();
        status = payment.getStatus();
        authCode = payment.getAuthCode();
        acquirerReferenceNumber = payment.getAcquirerReferenceNumber();
        riskScore = payment.getRiskScore();
        description = payment.getDescription();
        failureReason = payment.getFailureReason();
        refundAmount = payment.getRefundAmount();
        refundReason = payment.getRefundReason();
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
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

    public String getCardLastFour() {
        return cardLastFour;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
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

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getCardProvider() {
        return cardProvider;
    }

    public void setCardProvider(String cardProvider) {
        this.cardProvider = cardProvider;
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

    @JsonProperty("eventTime")
    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    @JsonProperty("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
