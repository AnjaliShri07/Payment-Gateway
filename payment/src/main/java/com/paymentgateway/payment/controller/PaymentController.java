package com.paymentgateway.payment.controller;

import com.paymentgateway.payment.client.dto.AuthUserProfile;
import com.paymentgateway.payment.document.PaymentAuditLog;
import com.paymentgateway.payment.dto.request.PaymentRequest;
import com.paymentgateway.payment.dto.request.RefundRequest;
import com.paymentgateway.payment.dto.request.TokenizeCardRequest;
import com.paymentgateway.payment.dto.response.PaymentAnalyticsResponse;
import com.paymentgateway.payment.dto.response.PaymentResponse;
import com.paymentgateway.payment.dto.response.TokenResponse;
import com.paymentgateway.payment.enums.PaymentStatus;
import com.paymentgateway.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Enterprise online CNP debit & credit card payment operations, tokenization, fraud checks, and lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(
        summary = "Initiate an online debit or credit card payment (CNP)",
        description = "Requires Bearer token from Auth microservice. Supports optional Idempotency-Key header to prevent duplicate charges and either card details or PCI-DSS cardToken."
    )
    public ResponseEntity<PaymentResponse> initiatePayment(
        @Valid @RequestBody PaymentRequest request,
        @AuthenticationPrincipal AuthUserProfile user,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
        @Parameter(description = "Unique UUID key to prevent duplicate charges upon retries")
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        log.info("Received payment initiation request from user: {} (IdempotencyKey: {})",
            user.username(), idempotencyKey);
        PaymentResponse response = paymentService.initiatePayment(request, user, authHeader, idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/tokenize")
    @Operation(
        summary = "Tokenize card for PCI-DSS compliance",
        description = "Replaces sensitive PAN with a secure token (tok_...). CVV is validated transiently and never vaulted."
    )
    public ResponseEntity<TokenResponse> tokenizeCard(
        @Valid @RequestBody TokenizeCardRequest request,
        @AuthenticationPrincipal AuthUserProfile user,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        log.info("Tokenizing card for user: {}", user.username());
        TokenResponse response = paymentService.tokenizeCard(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/refund")
    @Operation(
        summary = "Refund or reverse a completed/authorized transaction",
        description = "Refunds the specified amount (or full charge) and emits a payment.refunded Kafka event."
    )
    public ResponseEntity<PaymentResponse> refundPayment(
        @PathVariable String id,
        @RequestBody(required = false) RefundRequest request,
        @AuthenticationPrincipal AuthUserProfile user,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        log.info("Processing refund request for tx: {} by user: {}", id, user.username());
        PaymentResponse response = paymentService.refundPayment(id, request, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/capture")
    @Operation(
        summary = "Capture an authorized payment hold",
        description = "Transitions an AUTHORIZED transaction to COMPLETED."
    )
    public ResponseEntity<PaymentResponse> capturePayment(
        @PathVariable String id,
        @AuthenticationPrincipal AuthUserProfile user,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        log.info("Capturing payment for tx: {} by user: {}", id, user.username());
        PaymentResponse response = paymentService.capturePayment(id, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics")
    @Operation(
        summary = "Retrieve payment gateway analytics and performance metrics",
        description = "Returns real-time metrics including success rate, decline rate, total volume, and status distributions."
    )
    public ResponseEntity<PaymentAnalyticsResponse> getAnalytics(
        @AuthenticationPrincipal AuthUserProfile user,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        PaymentAnalyticsResponse analytics = paymentService.getAnalytics(user.id());
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get payment transaction details by ID",
        description = "Returns current transaction state (INITIATED, PROCESSING, AUTHORIZED, COMPLETED, FAILED, REFUNDED)."
    )
    public ResponseEntity<PaymentResponse> getPaymentById(
        @PathVariable String id,
        @AuthenticationPrincipal AuthUserProfile user,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        return paymentService.getPaymentById(id, user.id())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping
    @Operation(summary = "Get payments for authenticated user, optionally filtered by status")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
        @AuthenticationPrincipal AuthUserProfile user,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
        @RequestParam(required = false) PaymentStatus status
    ) {
        List<PaymentResponse> payments = status == null
            ? paymentService.getMyPayments(user.id())
            : paymentService.getMyPayments(user.id(), status);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}/audit-logs")
    @Operation(summary = "Get MongoDB audit logs for a payment transaction")
    public ResponseEntity<List<PaymentAuditLog>> getPaymentAuditLogs(
        @PathVariable String id,
        @AuthenticationPrincipal AuthUserProfile user,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        if (paymentService.getPaymentById(id, user.id()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        List<PaymentAuditLog> logs = paymentService.getPaymentAuditLogs(id);
        return ResponseEntity.ok(logs);
    }
}
