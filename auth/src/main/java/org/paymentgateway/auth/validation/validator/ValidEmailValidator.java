package org.paymentgateway.auth.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.paymentgateway.auth.validation.annotation.ValidEmail;
import org.paymentgateway.auth.validation.util.ValidationUtils;

public class ValidEmailValidator implements ConstraintValidator<ValidEmail, String> {

    @Override
    public void initialize(ValidEmail constraintAnnotation) {
        // No custom initialization needed
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null) {
            return false;
        }

        return ValidationUtils.isValidEmail(email);
    }
}
