package org.paymentgateway.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.paymentgateway.auth.validation.annotation.StrongPassword;
import org.paymentgateway.auth.validation.annotation.ValidEmail;
import org.paymentgateway.auth.validation.annotation.ValidUsername;

import java.util.Set;

public record RegisterRequest(
    @NotBlank(message = "Username cannot be blank")
    @ValidUsername
    String username,

    @NotBlank(message = "Email cannot be blank")
    @ValidEmail
    String email,

    @NotBlank(message = "Password cannot be blank")
    @StrongPassword
    String password,

    Set<String> roles
) {}
