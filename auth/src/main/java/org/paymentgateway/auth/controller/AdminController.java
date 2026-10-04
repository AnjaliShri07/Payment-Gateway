package org.paymentgateway.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.paymentgateway.auth.constants.AdminAccessRequestStatus;
import org.paymentgateway.auth.dto.request.AdminAccessDecision;
import org.paymentgateway.auth.dto.response.AdminAccessRequestResponse;
import org.paymentgateway.auth.dto.response.ApiResponse;
import org.paymentgateway.auth.dto.response.UserProfileResponse;
import org.paymentgateway.auth.security.JwtUserDetails;
import org.paymentgateway.auth.service.AdminAccessRequestService;
import org.paymentgateway.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for authentication service operations.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Endpoints restricted to users with ROLE_ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserService userService;
    private final AdminAccessRequestService adminAccessRequestService;

    public AdminController(UserService userService, AdminAccessRequestService adminAccessRequestService) {
        this.userService = userService;
        this.adminAccessRequestService = adminAccessRequestService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Admin Dashboard (Requires ROLE_ADMIN)")
    public ResponseEntity<ApiResponse<String>> adminDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Welcome to Admin Dashboard! Access granted."));
    }

    @GetMapping("/users")
    @Operation(summary = "Get all users in the system (Requires ROLE_ADMIN)")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAllUsers() {
        List<UserProfileResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Fetched all users successfully", users));
    }

    @GetMapping("/admin-access-requests")
    @Operation(summary = "Get pending administrator access requests")
    public ResponseEntity<ApiResponse<List<AdminAccessRequestResponse>>> getAdminAccessRequests() {
        return ResponseEntity.ok(ApiResponse.success(
            "Fetched pending administrator access requests",
            adminAccessRequestService.getPendingRequests()
        ));
    }

    @PostMapping("/admin-access-requests/{requestId}/decision")
    @Operation(summary = "Approve or reject an administrator access request")
    public ResponseEntity<ApiResponse<AdminAccessRequestResponse>> decideAdminAccessRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody AdminAccessDecision decision,
        @AuthenticationPrincipal JwtUserDetails reviewer
    ) {
        AdminAccessRequestResponse response = adminAccessRequestService.decideRequest(
            requestId,
            reviewer.getId(),
            decision.status(),
            decision.note()
        );
        return ResponseEntity.ok(ApiResponse.success("Administrator access request reviewed", response));
    }
}
