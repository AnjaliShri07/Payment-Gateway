package org.paymentgateway.auth.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.paymentgateway.auth.validation.annotation.StrongPassword;
import org.paymentgateway.auth.validation.util.ValidationUtils;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        // No custom initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        return ValidationUtils.isStrongPassword(password);
    }
}
