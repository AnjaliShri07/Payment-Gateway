package org.paymentgateway.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.paymentgateway.auth.dto.response.ApiResponse;
import org.paymentgateway.auth.dto.response.UserProfileResponse;
import org.paymentgateway.auth.exception.UnauthorizedException;
import org.paymentgateway.auth.security.CustomUserDetails;
import org.paymentgateway.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Endpoints for user profile and information")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/public")
    @Operation(summary = "Public content endpoint accessible without authentication")
    public ResponseEntity<ApiResponse<String>> publicContent() {
        return ResponseEntity.ok(ApiResponse.success("Public endpoint: Anyone can see this!"));
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get current authenticated user profile",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        UserProfileResponse profile = userService.getUserProfile(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", profile));
    }
}
