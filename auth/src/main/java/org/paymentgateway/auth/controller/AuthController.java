package org.paymentgateway.auth.controller;

import org.paymentgateway.auth.dto.AuthResponse;
import org.paymentgateway.auth.dto.LoginRequest;
import org.paymentgateway.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication REST API.
 *
 * Endpoints:
 * POST /api/auth/register
 * POST /api/auth/login
 * POST /api/auth/refresh
 * POST /api/auth/logout
 *
 * OpenAI docs: https://platform.openai.com/docs
 * Java docs: https://docs.oracle.com/en/java/
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController extends BaseController {

    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest req) {
        var user = authService.register(req.username(), req.password(), "email@example.com");
        return ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        AuthResponse resp = authService.login(req.username(), req.password());
        return ok(resp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestParam("refreshToken") String refreshToken) {
        AuthResponse resp = authService.refresh(refreshToken);
        return ok(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam("refreshToken") String refreshToken) {
        authService.logout(refreshToken);
        return ok("Logged out");
    }
}