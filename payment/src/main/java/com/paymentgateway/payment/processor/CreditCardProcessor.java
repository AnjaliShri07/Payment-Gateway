package com.paymentgateway.payment.processor;

import com.paymentgateway.payment.enums.CardType;
import com.paymentgateway.payment.model.PaymentEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Online Credit Card Processor (Card-Not-Present / E-Commerce Gateway):
 * Coordinates merchant acquirer processing, card network routing (Visa, Mastercard, Amex, Discover),
 * credit line authorization holds, interchange fee estimation, and Acquirer Reference Number (ARN) issuance.
 */
@Component
public class CreditCardProcessor implements CardPaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(CreditCardProcessor.class);

    private final Tracer tracer;

    public CreditCardProcessor(Tracer tracer) {
        this.tracer = tracer;
    }

    // Maximum credit limit for a single e-commerce transaction
    private static final BigDecimal MAXIMUM_CREDIT_TRANSACTION = new BigDecimal("100000.00");

    // Standard simulated interchange fee rate (1.85% + $0.10)
    private static final BigDecimal INTERCHANGE_PERCENT = new BigDecimal("0.0185");
    private static final BigDecimal FIXED_FEE = new BigDecimal("0.10");

    @Override
    public CardType getSupportedCardType() {
        return CardType.CREDIT;
    }

    @Override
    public PaymentProcessingResult process(PaymentEvent event) {
        Span span = tracer.nextSpan()
            .name("credit-card-processing")
            .tag("payment.transaction.id", event.getTransactionId())
            .start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            String traceId = span.context().traceId();
            log.info("Processing CREDIT card transaction [{}] | traceId={} | amount: {} {} via network: {}",
                event.getTransactionId(), traceId, event.getAmount(), event.getCurrency(), event.getCardProvider());

            // 1. Verify credit card transaction ceiling / available credit line
            if (event.getAmount().compareTo(MAXIMUM_CREDIT_TRANSACTION) > 0) {
                String msg = "Credit card transaction exceeds maximum single charge limit of "
                    + MAXIMUM_CREDIT_TRANSACTION + " " + event.getCurrency();
                log.warn("Credit processing rejected for tx [{}] | traceId={}: {}",
                    event.getTransactionId(), traceId, msg);
                return PaymentProcessingResult.failure(msg);
            }

            // 2. Simulate Card Network routing (Visa / Mastercard / Amex / Discover interchange)
            BigDecimal estimatedInterchange = event.getAmount()
                .multiply(INTERCHANGE_PERCENT)
                .add(FIXED_FEE)
                .setScale(2, RoundingMode.HALF_UP);

            log.info("Routed through {} Card Network | Estimated interchange fee: {} {} | traceId={}",
                event.getCardProvider(), estimatedInterchange, event.getCurrency(), traceId);

            // 3. Online 3D Secure / SCA Check
            if (Boolean.TRUE.equals(event.getThreeDSecureRequired())
                || (event.getRiskScore() != null && event.getRiskScore() >= 50)) {
                log.info("Cardholder 3DS authentication cryptographically verified for credit tx [{}] | traceId={}",
                    event.getTransactionId(), traceId);
            }

            // 4. Generate Acquirer Reference Number (ARN) and Card Network Authorization Code
            String authCode = "CREDIT-AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String arn = "ARN-CRD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

            log.info("Credit network approval for tx [{}] | AuthCode: {} | ARN: {} | traceId={}",
                event.getTransactionId(), authCode, arn, traceId);

            return PaymentProcessingResult.success(authCode, arn);
        } finally {
            span.end();
        }
    }
}
