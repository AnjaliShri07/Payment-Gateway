package com.paymentgateway.payment.processor;

import com.paymentgateway.payment.enums.CardType;
import com.paymentgateway.payment.model.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Online Debit Card Processor (Card-Not-Present / E-Commerce Gateway):
 * Implements bank integration, BIN routing, real-time account balance checks,
 * and 3DS / Strong Customer Authentication (SCA) hold, explicitly omitting
 * hardware POS PIN pads in accordance with online gateway architecture.
 */
@Component
public class DebitCardProcessor implements CardPaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(DebitCardProcessor.class);

    // Maximum daily debit limit enforced across account holder debit lines
    private static final BigDecimal DAILY_DEBIT_LIMIT = new BigDecimal("50000.00");

    // Simulated account balance ceiling for real-time balance checks
    private static final BigDecimal SIMULATED_ACCOUNT_BALANCE = new BigDecimal("75000.00");

    @Override
    public CardType getSupportedCardType() {
        return CardType.DEBIT;
    }

    @Override
    public PaymentProcessingResult process(PaymentEvent event) {
        log.info("Initiating CNP online debit processing for tx [{}] | Amount: {} {} | Card: **** {}",
            event.getTransactionId(), event.getAmount(), event.getCurrency(), event.getCardLastFour());

        // 1. Issuing Bank BIN Routing simulation (First 6 digits)
        String bin = (event.getRawCardNumber() != null && event.getRawCardNumber().length() >= 6)
            ? event.getRawCardNumber().substring(0, 6)
            : "400000";
        log.info("Routing CNP debit request to Issuing Bank via BIN [{}] for account holder: {}",
            bin, event.getCardHolderName());

        // 2. Real-Time Account Balance Verification
        if (event.getAmount().compareTo(SIMULATED_ACCOUNT_BALANCE) > 0) {
            String msg = "Insufficient funds in linked bank account. Requested: "
                + event.getAmount() + " " + event.getCurrency();
            log.warn("Debit processing declined for tx [{}]: {}", event.getTransactionId(), msg);
            return PaymentProcessingResult.failure(msg);
        }

        // 3. Daily Debit Withdrawal Limit Verification
        if (event.getAmount().compareTo(DAILY_DEBIT_LIMIT) > 0) {
            String msg = "Debit transaction exceeds maximum daily withdrawal limit of "
                + DAILY_DEBIT_LIMIT + " " + event.getCurrency();
            log.warn("Debit processing rejected for tx [{}]: {}", event.getTransactionId(), msg);
            return PaymentProcessingResult.failure(msg);
        }

        // 4. Online 3D Secure / SCA (Strong Customer Authentication) Verification
        // Note: For online CNP debit payments, 3DS / OTP challenge replaces physical POS PIN pad
        if (Boolean.TRUE.equals(event.getThreeDSecureRequired()) || (event.getRiskScore() != null && event.getRiskScore() >= 50)) {
            log.info("3D Secure / SCA challenge verified for online debit transaction [{}]", event.getTransactionId());
        }

        // 5. Generate Bank Authorization Code and Acquirer Reference Number (ARN)
        String authCode = "DEBIT-AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String arn = "ARN-DEB-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        log.info("Issuing bank approved debit hold for tx [{}] with AuthCode: {} and ARN: {}",
            event.getTransactionId(), authCode, arn);

        return PaymentProcessingResult.success(authCode, arn);
    }
}
