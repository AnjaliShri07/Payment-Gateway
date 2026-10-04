package org.paymentgateway.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.paymentgateway.auth.validation.annotation.StrongPassword;

public record ChangePasswordRequest(
    @NotBlank(message = "Current password is required")
    String currentPassword,

    @NotBlank(message = "New password is required")
    @StrongPassword
    String newPassword
) {}
