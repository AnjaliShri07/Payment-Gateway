package org.paymentgateway.auth.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.paymentgateway.auth.validation.validator.ValidUsernameValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidUsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUsername {

    String message() default "Username must be 3-50 characters, contain only letters, digits, underscores, or hyphens, and cannot be a reserved name";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
