package com.brewdeck.brewdeck_api.common.error;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.util.HtmlUtils;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
      EntityNotFoundException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.NOT_FOUND,
            sanitize(exception.getMessage()),
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    Map<String, String> validationErrors = new HashMap<>();

    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(
            fieldError ->
                validationErrors.put(
                    sanitize(fieldError.getField()), sanitize(fieldError.getDefaultMessage())));

    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            sanitize(request.getRequestURI()),
            validationErrors);

    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.CONFLICT,
            "Data integrity violation",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected error occurred",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  private ErrorResponse buildErrorResponse(
      HttpStatus status, String message, String path, Map<String, String> validationErrors) {
    return new ErrorResponse(
        LocalDateTime.now(),
        status.value(),
        status.getReasonPhrase(),
        message,
        path,
        validationErrors);
  }

  private String sanitize(String value) {
    if (value == null) {
      return null;
    }

    return HtmlUtils.htmlEscape(value);
  }
}
