package org.paymentgateway.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAccessRequestPayload(
    @NotBlank(message = "A reason is required")
    @Size(max = 500, message = "The reason cannot exceed 500 characters")
    String reason
) {}
