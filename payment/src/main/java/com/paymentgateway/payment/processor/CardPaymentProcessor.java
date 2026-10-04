package com.paymentgateway.payment.processor;


import com.paymentgateway.payment.enums.CardType;
import com.paymentgateway.payment.model.PaymentEvent;

public interface CardPaymentProcessor {

    CardType getSupportedCardType();

    PaymentProcessingResult process(PaymentEvent event);
}
