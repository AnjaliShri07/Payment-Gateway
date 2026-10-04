package com.paymentgateway.payment.document;

import com.paymentgateway.payment.enums.PaymentStatus;
import com.paymentgateway.payment.model.PaymentDetails;
import com.paymentgateway.payment.model.PaymentEvent;
import com.paymentgateway.payment.model.PaymentPayload;

import java.time.Instant;

public class PaymentEventDocument {

    private String id;
    private String eventType;
    private PaymentPayload payload;
    private PaymentDetails payment;
    private Instant eventTime;
    private Instant storedAt;

    public PaymentEventDocument() {
    }

    public static PaymentEventDocument from(PaymentEvent event, String eventType) {
        PaymentEventDocument document = new PaymentEventDocument();
        document.eventType = eventType;
        document.payload = event.getPayload();
        document.payment = event.getPayment();
        document.payment.setRawCardNumber(null);
        document.payment.setCvv(null);
        document.payment.setExpiryMonth(null);
        document.payment.setExpiryYear(null);
        document.eventTime = event.getTimestamp();
        document.storedAt = Instant.now();
        return document;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public PaymentPayload getPayload() { return payload; }
    public void setPayload(PaymentPayload payload) { this.payload = payload; }
    public PaymentDetails getPayment() { return payment; }
    public void setPayment(PaymentDetails payment) { this.payment = payment; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public Instant getStoredAt() { return storedAt; }
    public void setStoredAt(Instant storedAt) { this.storedAt = storedAt; }

    public String getTransactionId() { return payment != null ? payment.getTransactionId() : null; }
    public Long getUserId() { return payload != null ? payload.userId() : null; }
    public java.math.BigDecimal getAmount() { return payment != null ? payment.getAmount() : null; }
    public String getCurrency() { return payment != null ? payment.getCurrency() : null; }
    public PaymentStatus getStatus() { return payment != null ? payment.getStatus() : null; }
    public String getAuthCode() { return payment != null ? payment.getAuthCode() : null; }
    public String getAcquirerReferenceNumber() { return payment != null ? payment.getAcquirerReferenceNumber() : null; }
    public Integer getRiskScore() { return payment != null ? payment.getRiskScore() : null; }
    public String getDescription() { return payment != null ? payment.getDescription() : null; }
    public String getFailureReason() { return payment != null ? payment.getFailureReason() : null; }
    public java.math.BigDecimal getRefundAmount() { return payment != null ? payment.getRefundAmount() : null; }
    public String getRefundReason() { return payment != null ? payment.getRefundReason() : null; }
}
