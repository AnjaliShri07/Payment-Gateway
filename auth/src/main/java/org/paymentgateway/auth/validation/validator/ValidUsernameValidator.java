package org.paymentgateway.auth.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.paymentgateway.auth.validation.annotation.ValidUsername;
import org.paymentgateway.auth.validation.util.ValidationUtils;

public class ValidUsernameValidator implements ConstraintValidator<ValidUsername, String> {

    @Override
    public void initialize(ValidUsername constraintAnnotation) {
        // No custom initialization needed
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null) {
            return false;
        }

        return ValidationUtils.isValidUsername(username);
    }
}
