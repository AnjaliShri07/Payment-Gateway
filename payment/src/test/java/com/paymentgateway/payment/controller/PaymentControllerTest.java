package com.paymentgateway.payment.controller;

import com.paymentgateway.payment.client.AuthServiceClient;
import com.paymentgateway.payment.client.dto.AuthUserProfile;
import com.paymentgateway.payment.config.SecurityConfig;
import com.paymentgateway.payment.document.PaymentAuditLog;
import com.paymentgateway.payment.dto.response.PaymentAnalyticsResponse;
import com.paymentgateway.payment.dto.response.PaymentResponse;
import com.paymentgateway.payment.dto.response.TokenResponse;
import com.paymentgateway.payment.enums.CardProvider;
import com.paymentgateway.payment.enums.CardType;
import com.paymentgateway.payment.enums.PaymentStatus;
import com.paymentgateway.payment.exception.PaymentFailureException;
import com.paymentgateway.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    private static final String BASE_URL = "/api/v1/payments";
    private static final String AUTH_HEADER = "Bearer test-token";
    private static final AuthUserProfile USER = new AuthUserProfile(
        42L, "api-test-user", "api-test@example.com", List.of("ROLE_USER"),
        true, Instant.now(), Instant.now()
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private AuthServiceClient authServiceClient;

    @BeforeEach
    void authenticateRequests() {
        when(authServiceClient.getAuthenticatedUser(AUTH_HEADER)).thenReturn(Optional.of(USER));
    }

    @Test
    void initiatePaymentReturnsAccepted() throws Exception {
        when(paymentService.initiatePayment(any(), eq(USER), eq(AUTH_HEADER), eq("request-1")))
            .thenReturn(paymentResponse());

        mockMvc.perform(post(BASE_URL)
                .header("Authorization", AUTH_HEADER)
                .header("Idempotency-Key", "request-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"cardType":"CREDIT","cardNumber":"4532015112830366","cardHolderName":"Test User",
                     "expiryMonth":"12","expiryYear":"2030","cvv":"123","amount":25.00,"currency":"USD"}
                    """))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.transactionId").value("tx-api-1"));
    }

    @Test
    void tokenizeCardReturnsToken() throws Exception {
        when(paymentService.tokenizeCard(any())).thenReturn(new TokenResponse(
            "tok_visa_test", "0366", CardProvider.VISA, CardType.CREDIT,
            "Test User", "12", "2030", Instant.now()
        ));

        mockMvc.perform(post(BASE_URL + "/tokenize")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"cardType":"CREDIT","cardNumber":"4532015112830366","cardHolderName":"Test User",
                     "expiryMonth":"12","expiryYear":"2030","cvv":"123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cardToken").value("tok_visa_test"));
    }

    @Test
    void paymentApiAllowsConfiguredFrontendPreflight() throws Exception {
        mockMvc.perform(options(BASE_URL + "/tokenize")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void refundPaymentReturnsUpdatedPayment() throws Exception {
        when(paymentService.refundPayment(eq("tx-api-1"), any(), eq(USER)))
            .thenReturn(paymentResponse());

        mockMvc.perform(post(BASE_URL + "/tx-api-1/refund")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10.00,\"reason\":\"Customer request\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactionId").value("tx-api-1"));
    }

    @Test
    void capturePaymentReturnsUpdatedPayment() throws Exception {
        when(paymentService.capturePayment("tx-api-1", USER)).thenReturn(paymentResponse());

        mockMvc.perform(post(BASE_URL + "/tx-api-1/capture")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactionId").value("tx-api-1"));
    }

    @Test
    void capturePaymentReturnsFailureMessageWhenPaymentFailed() throws Exception {
        when(paymentService.capturePayment("tx-api-1", USER))
            .thenThrow(new PaymentFailureException("Payment failed: Insufficient funds"));

        mockMvc.perform(post(BASE_URL + "/tx-api-1/capture")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("Payment Failed"))
            .andExpect(jsonPath("$.message").value("Payment failed: Insufficient funds"));
    }

    @Test
    void analyticsReturnsMetrics() throws Exception {
        when(paymentService.getAnalytics(USER.id())).thenReturn(new PaymentAnalyticsResponse(
            2, 1, 1, 0, 0, 50.0, 50.0, new BigDecimal("25.00"), "USD", Map.of()
        ));

        mockMvc.perform(get(BASE_URL + "/analytics").header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalTransactions").value(2))
            .andExpect(jsonPath("$.successRatePercentage").value(50.0));
    }

    @Test
    void getPaymentReturnsNotFoundWhenNotOwnedOrMissing() throws Exception {
        when(paymentService.getPaymentById("missing", USER.id())).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL + "/missing").header("Authorization", AUTH_HEADER))
            .andExpect(status().isNotFound());
    }

    @Test
    void listPaymentsReturnsCurrentUsersPayments() throws Exception {
        when(paymentService.getMyPayments(USER.id())).thenReturn(List.of(paymentResponse()));

        mockMvc.perform(get(BASE_URL).header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].transactionId").value("tx-api-1"));
    }

    @Test
    void listPaymentsCanFilterByStatus() throws Exception {
        when(paymentService.getMyPayments(USER.id(), PaymentStatus.COMPLETED))
            .thenReturn(List.of(paymentResponse()));

        mockMvc.perform(get(BASE_URL)
                .param("status", "COMPLETED")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].transactionId").value("tx-api-1"));
    }

    @Test
    void listPaymentsRejectsUnknownStatus() throws Exception {
        mockMvc.perform(get(BASE_URL)
                .param("status", "UNKNOWN")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid value for parameter 'status'"));
    }

    @Test
    void auditLogsReturnsTransactionHistory() throws Exception {
        PaymentAuditLog auditLog = new PaymentAuditLog();
        auditLog.setTransactionId("tx-api-1");
        auditLog.setUserId(USER.id());
        when(paymentService.getPaymentById("tx-api-1", USER.id())).thenReturn(Optional.of(paymentResponse()));
        when(paymentService.getPaymentAuditLogs("tx-api-1")).thenReturn(List.of(auditLog));

        mockMvc.perform(get(BASE_URL + "/tx-api-1/audit-logs").header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].payload.userId").value(USER.id()))
            .andExpect(jsonPath("$[0].payment.transactionId").value("tx-api-1"))
            .andExpect(jsonPath("$[0].transactionId").doesNotExist());
    }

    @Test
    void paymentRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void initiatePaymentRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.path").value(BASE_URL))
            .andExpect(jsonPath("$.validationErrors.cardType").exists());
    }

    private PaymentResponse paymentResponse() {
        return new PaymentResponse(
            "tx-api-1", USER.id(), "request-1", "Test User", "0366", "tok_visa_test",
            CardType.CREDIT, CardProvider.VISA, new BigDecimal("25.00"), "USD",
            PaymentStatus.INITIATED, null, null, null, null, null, null, null,
            Instant.now(), null, null
        );
    }
}
