package org.paymentgateway.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.paymentgateway.auth.constants.AdminAccessRequestStatus;

public record AdminAccessDecision(
    @NotNull(message = "A decision is required")
    AdminAccessRequestStatus status,

    @Size(max = 500, message = "The decision note cannot exceed 500 characters")
    String note
) {}
