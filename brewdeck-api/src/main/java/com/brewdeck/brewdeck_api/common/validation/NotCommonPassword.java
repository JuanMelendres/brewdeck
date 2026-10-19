package com.brewdeck.brewdeck_api.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rejects a new password found in the local list of common passwords (ADR-018, NIST SP 800-63B).
 * Only for passwords being set (register, change, reset), never for login.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotCommonPasswordValidator.class)
public @interface NotCommonPassword {

  String message() default "{validation.password.common}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
