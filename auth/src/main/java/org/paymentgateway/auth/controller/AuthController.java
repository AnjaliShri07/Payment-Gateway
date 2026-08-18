package org.paymentgateway.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.paymentgateway.auth.dto.request.LoginRequest;
import org.paymentgateway.auth.dto.request.LogoutRequest;
import org.paymentgateway.auth.dto.request.RegisterRequest;
import org.paymentgateway.auth.dto.request.TokenRefreshRequest;
import org.paymentgateway.auth.dto.response.ApiResponse;
import org.paymentgateway.auth.dto.response.AuthResponse;
import org.paymentgateway.auth.dto.response.TokenRefreshResponse;
import org.paymentgateway.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, token refresh, and logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<Void>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("User registered successfully!"));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and generate Access & Refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful!", authResponse));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh JWT Access Token using a valid Refresh Token (with Token Rotation)")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully!", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate the active Refresh Token")
    public ResponseEntity<ApiResponse<Void>> logoutUser(@Valid @RequestBody LogoutRequest logoutRequest) {
        authService.logout(logoutRequest);
        return ResponseEntity.ok(ApiResponse.success("User logged out successfully!"));
    }
}
