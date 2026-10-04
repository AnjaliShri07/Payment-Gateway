package com.paymentgateway.payment.dto.response;

import com.paymentgateway.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.Map;

public record PaymentAnalyticsResponse(
    long totalTransactions,
    long completedTransactions,
    long failedTransactions,
    long refundedTransactions,
    long authorizedTransactions,
    double successRatePercentage,
    double declineRatePercentage,
    BigDecimal totalProcessedVolume,
    String primaryCurrency,
    Map<PaymentStatus, Long> statusBreakdown
) {}
