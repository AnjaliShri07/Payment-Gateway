package org.paymentgateway.auth.validation.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.paymentgateway.auth.exception.BadRequestException;
import org.paymentgateway.auth.validation.util.ValidationUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class FieldValidationService {

    private final Validator validator;

    public FieldValidationService(Validator validator) {
        this.validator = validator;
    }

    /**
     * Programmatically validates any object using Bean Validation.
     *
     * @param object The object to validate
     * @param <T>    The object type
     * @return Map of property path to violation message
     */
    public <T> Map<String, String> validate(T object) {
        if (object == null) {
            Map<String, String> error = new HashMap<>();
            error.put("payload", "Request payload cannot be null");
            return error;
        }

        Set<ConstraintViolation<T>> violations = validator.validate(object);
        Map<String, String> errors = new HashMap<>();

        for (ConstraintViolation<T> violation : violations) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        }

        return errors;
    }

    /**
     * Validates an object and throws a BadRequestException if any violations exist.
     */
    public <T> void validateAndThrow(T object) {
        Map<String, String> errors = validate(object);
        if (!errors.isEmpty()) {
            String firstError = errors.values().iterator().next();
            throw new BadRequestException("Validation error: " + firstError);
        }
    }

    /**
     * Validates user registration fields individually and returns all errors.
     */
    public Map<String, String> validateRegistrationFields(String username, String email, String password) {
        Map<String, String> errors = new HashMap<>();

        if (!ValidationUtils.isValidUsername(username)) {
            if (ValidationUtils.isReservedUsername(username)) {
                errors.put("username", "The username '" + username + "' is reserved and cannot be registered.");
            } else {
                errors.put("username", "Username must be 3-50 characters and contain only letters, numbers, hyphens, or underscores.");
            }
        }

        if (!ValidationUtils.isValidEmail(email)) {
            errors.put("email", "Invalid email format. Please provide a valid RFC 5322 email address.");
        }

        if (!ValidationUtils.isStrongPassword(password)) {
            errors.put("password", "Password must be 8-40 characters long and include uppercase, lowercase, digit, special character, and no whitespace.");
        }

        return errors;
    }
}
