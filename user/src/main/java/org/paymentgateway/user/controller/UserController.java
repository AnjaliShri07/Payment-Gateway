package org.paymentgateway.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.paymentgateway.user.DTO.BaseResponse;
import org.paymentgateway.user.entity.User;
import org.paymentgateway.user.service.UserServiceImpl;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "CRUD operations for users")
public class UserController extends BaseController<User, Long> {

    private final UserServiceImpl userServiceImpl;

    public UserController(UserServiceImpl userServiceImpl) {
        super(userServiceImpl);
        this.userServiceImpl = userServiceImpl;
    }

    /*@Operation(summary = "Get all users", description = "Fetches all users from the database")

    @GetMapping
    public BaseResponse<List<User>> getAllUsers() {
        return BaseResponse.success("Fetched all users", userService.findAll());
    }

    @Operation(summary = "Get user by ID", description = "Fetches a single user by their ID")
    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "fallbackGetUserById")
    @GetMapping("/{id}")
    public BaseResponse<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return user != null
                ? BaseResponse.success("User found", user)
                : BaseResponse.error("User not found", null);
    }

    @Operation
            (summary = "Create user", description = "Creates a new user record")
    @PostMapping
    public BaseResponse<User> createUser(@RequestBody User user) {
        return BaseResponse.success("User created successfully", userService.save(user));
    }

    @Operation(summary = "Update user", description = "Updates an existing user record")
    @PutMapping("/{id}")
    public BaseResponse<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        User updated = userService.update(id, user);
        return updated != null
                ? BaseResponse.success("User updated successfully", updated)
                : BaseResponse.error("Update failed, user not found", null);
    }*/


    // --- Fallback methods ---
    public BaseResponse<List<User>> fallbackGetAllUsers(Throwable t) {
        return BaseResponse.error("Circuit breaker triggered: " + t.getMessage(), List.of());
    }

    public BaseResponse<User> fallbackGetUserById(Long id, Throwable t) {
        return BaseResponse.error("Circuit breaker triggered: " + t.getMessage(), null);
    }
}
