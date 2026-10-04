package com.paymentgateway.payment.processor;

import com.paymentgateway.payment.enums.PaymentStatus;

public record PaymentProcessingResult(
    boolean success,
    String failureReason,
    String authCode,
    String acquirerReferenceNumber,
    PaymentStatus resultingStatus
) {
    public static PaymentProcessingResult success(String authCode, String acquirerReferenceNumber) {
        return new PaymentProcessingResult(true, null, authCode, acquirerReferenceNumber, PaymentStatus.COMPLETED);
    }

    public static PaymentProcessingResult authorized(String authCode, String acquirerReferenceNumber) {
        return new PaymentProcessingResult(true, null, authCode, acquirerReferenceNumber, PaymentStatus.AUTHORIZED);
    }

    public static PaymentProcessingResult failure(String failureReason) {
        return new PaymentProcessingResult(false, failureReason, null, null, PaymentStatus.FAILED);
    }
}
