package com.brewdeck.brewdeck_api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotCommonPasswordValidator implements ConstraintValidator<NotCommonPassword, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // Null and blank are @NotBlank's job.
    return value == null || !CommonPasswords.contains(value);
  }
}
