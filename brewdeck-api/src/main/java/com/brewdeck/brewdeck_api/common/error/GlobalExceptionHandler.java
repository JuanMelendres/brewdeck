package com.brewdeck.brewdeck_api.common.error;

import com.brewdeck.brewdeck_api.ai.AiUnavailableException;
import com.brewdeck.brewdeck_api.ai.InsufficientBrewHistoryException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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
    Map<String, String> validationErrors = new LinkedHashMap<>();

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

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableMessage(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Malformed request body",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Invalid value for parameter '" + sanitize(exception.getName()) + "'",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(PropertyReferenceException.class)
  public ResponseEntity<ErrorResponse> handleInvalidSortProperty(
      PropertyReferenceException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Invalid sort property '" + sanitize(exception.getPropertyName()) + "'",
            sanitize(request.getRequestURI()),
            null);

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

  @ExceptionHandler(AiUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleAiUnavailable(
      AiUnavailableException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE,
            "AI suggestion service is unavailable",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
  }

  @ExceptionHandler(InsufficientBrewHistoryException.class)
  public ResponseEntity<ErrorResponse> handleInsufficientBrewHistory(
      InsufficientBrewHistoryException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Recipe has no rated brew sessions to improve from",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
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
