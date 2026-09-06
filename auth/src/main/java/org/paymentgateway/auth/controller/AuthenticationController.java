package org.paymentgateway.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.paymentgateway.auth.dto.request.*;
import org.paymentgateway.auth.dto.response.*;
import org.paymentgateway.auth.service.AuthenticationService;
import org.paymentgateway.auth.service.ClientApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, token refresh, and logout")
public class AuthenticationController {

    private final AuthenticationService authService;
    private final ClientApplicationService clientApplicationService;

    public AuthenticationController(AuthenticationService authService, ClientApplicationService clientApplicationService) {
        this.authService = authService;
        this.clientApplicationService = clientApplicationService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<Void>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("User registered successfully!"));
    }

   /* @PostMapping("/token")
    public ResponseEntity<String> generateToken(@RequestParam String username) {
        String token = authService.generateAccessToken(username);
        return ResponseEntity.ok(token);
    }*/

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

    // --- Client (machine-to-machine) endpoints ---

    @PostMapping("/register-client")
    @Operation(summary = "Register a machine client (returns client_id and client_secret). Store the secret securely; it is shown only once on creation.")
    public ResponseEntity<ApiResponse<ClientRegisterResponse>> registerClient(@Valid @RequestBody org.paymentgateway.auth.dto.request.ClientRegisterRequest request) {
        ClientRegisterResponse resp = clientApplicationService.registerClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Client registered successfully!", resp));
    }

    @PostMapping("/token")
    @Operation(summary = "Issue access token using client_credentials grant")
    public ResponseEntity<ApiResponse<ClientTokenResponse>> issueClientToken(@Valid @RequestBody org.paymentgateway.auth.dto.request.ClientTokenRequest request) {
        if (request.grantType() != null && !"client_credentials".equalsIgnoreCase(request.grantType())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Unsupported grant_type. Use client_credentials."));
        }
        ClientTokenResponse tokenResp = clientApplicationService.issueToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token issued successfully!", tokenResp));
    }

}
