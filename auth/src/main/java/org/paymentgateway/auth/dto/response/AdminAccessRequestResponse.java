package org.paymentgateway.auth.dto.response;

import org.paymentgateway.auth.constants.AdminAccessRequestStatus;

import java.time.Instant;

public record AdminAccessRequestResponse(
    Long id,
    Long userId,
    String username,
    String email,
    String reason,
    AdminAccessRequestStatus status,
    Instant requestedAt,
    Instant reviewedAt,
    String reviewedBy,
    String decisionNote
) {}
