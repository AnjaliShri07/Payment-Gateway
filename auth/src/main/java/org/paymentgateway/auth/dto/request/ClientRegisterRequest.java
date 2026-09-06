package org.paymentgateway.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record ClientRegisterRequest(
    @NotBlank(message = "Client name is required")
    String name,
    Set<String> scopes
) {}
