package org.paymentgateway.user.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.paymentgateway.user.DTO.BaseResponse;
import org.paymentgateway.user.client.AuthenticationServiceClient;
import org.paymentgateway.user.client.dto.AuthApiResponse;
import org.paymentgateway.user.client.dto.AuthUserProfile;
import org.paymentgateway.user.DTO.UserUpdateRequest;
import org.paymentgateway.user.entity.User;
import org.paymentgateway.user.service.UserServiceImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller exposing user-management endpoints for the payment gateway.
 * Handles retrieval, update, and deletion of users while validating the incoming
 * Authorization header through the authentication service before performing the
 * requested operation.
 *
 * <p>All endpoints are versioned under {@code /api/v1/users} and return a
 * standardized {@link BaseResponse} payload.</p>
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User CRUD + Authentication endpoints")
@Slf4j
@Validated
public class UserController {

    private final UserServiceImpl userService;
    private final AuthenticationServiceClient authenticationServiceClient;

    public UserController(UserServiceImpl userService, AuthenticationServiceClient authenticationServiceClient) {
        this.userService = userService;
        this.authenticationServiceClient = authenticationServiceClient;
    }


    @Operation(summary = "Get all users")
    @ApiResponse(responseCode = "200", description = "Users fetched successfully")
    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "fallbackGetAllUsers")
    @GetMapping
    public ResponseEntity<BaseResponse<List<User>>> getAll(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        log.info("Fetching all users");
        Optional<AuthUserProfile> authUserProfile = authenticationServiceClient.getAuthenticatedUser(authHeader);
        if (authUserProfile.isPresent()) {
            log.info("Verified user profile against Auth microservice");
        }
        return ResponseEntity.ok(BaseResponse.success("Fetched all records", userService.findAll()));
    }

    @Operation(summary = "Get user by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "fallbackGetUserById")
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<User>> getById(@PathVariable Long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        log.info("Fetching user with id: {}", id);
        Optional<AuthUserProfile> authUserProfile = authenticationServiceClient.getAuthenticatedUser(authHeader);
        if (authUserProfile.isPresent()) {
            log.info("Verified user profile against Auth microservice for user ID: {}", id);
        }
        User entity = userService.findById(id);
        if (entity != null) {
            return ResponseEntity.ok(BaseResponse.success("Record found", entity));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error("User not found", null));
    }

    @Operation(summary = "Update user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<User>> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest entity, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        log.info("Updating user with id: {}", id);
        Optional<AuthUserProfile> authUserProfile = authenticationServiceClient.getAuthenticatedUser(authHeader);
        if (authUserProfile.isPresent()) {
            log.info("Verified user profile against Auth microservice for user ID: {}", id);
        }
        User updated = userService.update(id, entity);
        if (updated != null) {
            return ResponseEntity.ok(BaseResponse.success("Record updated successfully", updated));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error("User not found", null));
    }

    @Operation(summary = "Delete user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable Long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        log.info("Deleting user with id: {}", id);
        Optional<AuthUserProfile> authUserProfile = authenticationServiceClient.getAuthenticatedUser(authHeader);
        if (authUserProfile.isPresent()) {
            log.info("Verified user profile against Auth microservice for user ID: {}", id);
        }
        userService.delete(id);
        return ResponseEntity.ok(BaseResponse.success("Record deleted successfully", null));
    }

    // ==================== FALLBACK METHODS ====================

    public ResponseEntity<BaseResponse<List<User>>> fallbackGetAllUsers(Throwable t) {
        log.error("Circuit breaker triggered for getAll: {}", t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(BaseResponse.error("Service temporarily unavailable, please try again later", List.of()));
    }

    public ResponseEntity<BaseResponse<User>> fallbackGetUserById(Long id, Throwable t) {
        log.error("Circuit breaker triggered for getById with id: {}", id);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(BaseResponse.error("Service temporarily unavailable, please try again later", null));
    }
}