package com.paymentgateway.payment.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.paymentgateway.payment.enums.CardProvider;
import com.paymentgateway.payment.enums.CardType;
import com.paymentgateway.payment.enums.PaymentStatus;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentDetails {

    private String transactionId;
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
    private String rawCardNumber;
    private String expiryMonth;
    private String expiryYear;
    private String cvv;

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }
    public String getCardLastFour() { return cardLastFour; }
    public void setCardLastFour(String cardLastFour) { this.cardLastFour = cardLastFour; }
    public String getCardToken() { return cardToken; }
    public void setCardToken(String cardToken) { this.cardToken = cardToken; }
    public CardType getCardType() { return cardType; }
    public void setCardType(CardType cardType) { this.cardType = cardType; }
    public CardProvider getCardProvider() { return cardProvider; }
    public void setCardProvider(CardProvider cardProvider) { this.cardProvider = cardProvider; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }
    public String getAcquirerReferenceNumber() { return acquirerReferenceNumber; }
    public void setAcquirerReferenceNumber(String acquirerReferenceNumber) { this.acquirerReferenceNumber = acquirerReferenceNumber; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public Boolean getThreeDSecureRequired() { return threeDSecureRequired; }
    public void setThreeDSecureRequired(Boolean threeDSecureRequired) { this.threeDSecureRequired = threeDSecureRequired; }
    public String getRawCardNumber() { return rawCardNumber; }
    public void setRawCardNumber(String rawCardNumber) { this.rawCardNumber = rawCardNumber; }
    public String getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(String expiryMonth) { this.expiryMonth = expiryMonth; }
    public String getExpiryYear() { return expiryYear; }
    public void setExpiryYear(String expiryYear) { this.expiryYear = expiryYear; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
}
