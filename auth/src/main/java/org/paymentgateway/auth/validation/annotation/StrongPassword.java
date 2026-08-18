package org.paymentgateway.auth.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.paymentgateway.auth.validation.validator.StrongPasswordValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Password must be 8-40 characters long and contain at least one uppercase letter, one lowercase letter, one digit, one special character, and no whitespace";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
