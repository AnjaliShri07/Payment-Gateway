package org.paymentgateway.user.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.paymentgateway.user.DTO.BaseResponse;
import org.paymentgateway.user.entity.User;
import org.paymentgateway.user.service.BaseService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
public abstract class BaseController<T, ID> {

    protected final BaseService<T, ID> service;

    protected BaseController(BaseService<T, ID> service) {
        this.service = service;
    }

    @Operation(summary = "Get all users", description = "Fetches all users from the database")
    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "fallbackGetAllUsers")
    @GetMapping
    public BaseResponse<List<T>> getAll() {
        return BaseResponse.success("Fetched all records", service.findAll());
    }

    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "fallbackGetUserById")
    @GetMapping("/{id}")
    public BaseResponse<T> getById(@PathVariable ID id) {
        T entity = service.findById(id);
        if (entity != null) {
            return BaseResponse.success("Record found", entity);
        }
        return BaseResponse.error("Record not found", null);
    }

    @PostMapping
    public BaseResponse<T> create(@RequestBody T entity) {
        return BaseResponse.success("Record created successfully", service.save(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<T> update(@PathVariable ID id, @RequestBody T entity) {
        T updated = service.update(id, entity);
        if (updated != null) {
            return BaseResponse.success("Record updated successfully", updated);
        }
        return BaseResponse.error("Update failed, record not found", null);
    }

    @Operation(summary = "Delete user", description = "Deletes a user record by ID")
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable ID id) {
        service.delete(id);
        return BaseResponse.success("Record deleted successfully", null);
    }

    // --- Fallback methods ---
    public BaseResponse<List<User>> fallbackGetAllUsers(Throwable t) {
        return BaseResponse.error("Circuit breaker triggered: " + t.getMessage(), List.of());
    }

    public BaseResponse<User> fallbackGetUserById(Long id, Throwable t) {
        return BaseResponse.error("Circuit breaker triggered: " + t.getMessage(), null);
    }
}
