package com.brewdeck.brewdeck_api.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated string must encode to at most {@link #value()} bytes in UTF-8. Use it where a limit
 * is on bytes rather than characters, e.g. BCrypt passwords (72 bytes): {@code @Size} counts
 * characters, so a password of accented letters or emoji can pass {@code @Size(max = 72)} and still
 * exceed 72 bytes. {@code null} is valid; combine with {@code @NotBlank} when required.
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

  int value();

  String message() default "must not exceed {value} bytes";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
