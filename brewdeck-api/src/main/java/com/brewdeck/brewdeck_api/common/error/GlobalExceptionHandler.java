package com.brewdeck.brewdeck_api.common.error;

import com.brewdeck.brewdeck_api.ai.AiUnavailableException;
import com.brewdeck.brewdeck_api.ai.InsufficientBrewHistoryException;
import com.brewdeck.brewdeck_api.auth.EmailAlreadyUsedException;
import com.brewdeck.brewdeck_api.auth.InvalidCurrentPasswordException;
import com.brewdeck.brewdeck_api.auth.refresh.InvalidRefreshTokenException;
import com.brewdeck.brewdeck_api.auth.reset.InvalidResetTokenException;
import com.brewdeck.brewdeck_api.auth.verification.InvalidVerificationTokenException;
import com.brewdeck.brewdeck_api.featureflag.FeatureDisabledException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.util.HtmlUtils;

@RestControllerAdvice
@Slf4j
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

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentials(
      BadCredentialsException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "Invalid email or password",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  @ExceptionHandler(InvalidRefreshTokenException.class)
  public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
      InvalidRefreshTokenException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "Refresh token is invalid or has expired",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  @ExceptionHandler(InvalidVerificationTokenException.class)
  public ResponseEntity<ErrorResponse> handleInvalidVerificationToken(
      InvalidVerificationTokenException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Verification token is invalid or has expired",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(InvalidResetTokenException.class)
  public ResponseEntity<ErrorResponse> handleInvalidResetToken(
      InvalidResetTokenException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Reset token is invalid or has expired",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(InvalidCurrentPasswordException.class)
  public ResponseEntity<ErrorResponse> handleInvalidCurrentPassword(
      InvalidCurrentPasswordException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Current password is incorrect",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      AccessDeniedException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.FORBIDDEN,
            "Insufficient permissions",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
  }

  @ExceptionHandler(EmailAlreadyUsedException.class)
  public ResponseEntity<ErrorResponse> handleEmailAlreadyUsed(
      EmailAlreadyUsedException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.CONFLICT,
            sanitize(exception.getMessage()),
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
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

  @ExceptionHandler(FeatureDisabledException.class)
  public ResponseEntity<ErrorResponse> handleFeatureDisabled(
      FeatureDisabledException exception, HttpServletRequest request) {
    // Status is chosen by flag type (404 for release/experiment/permission, 503 for
    // operational/kill-switch). The message is deliberately generic so a disabled feature is not
    // discoverable from the response body.
    HttpStatus status = exception.getStatus();
    String message =
        status == HttpStatus.SERVICE_UNAVAILABLE
            ? "This feature is temporarily unavailable"
            : "This feature is not available";
    ErrorResponse errorResponse =
        buildErrorResponse(status, message, sanitize(request.getRequestURI()), null);

    return ResponseEntity.status(status).body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception exception, HttpServletRequest request) {
    // Spring MVC's own client errors (405 method not allowed, 404 no handler/resource, 415/406
    // media type, 400 missing parameter, ResponseStatusException, ...) all implement Spring's
    // ErrorResponse contract. Keep their status and headers (e.g. Allow on 405) instead of
    // flattening them into a 500.
    if (exception instanceof org.springframework.web.ErrorResponse springError) {
      return handleSpringWebError(springError, exception, request);
    }

    log.error(
        "Unhandled exception on {} {}",
        request.getMethod(),
        sanitizeForLog(request.getRequestURI()),
        exception);
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected error occurred",
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  private ResponseEntity<ErrorResponse> handleSpringWebError(
      org.springframework.web.ErrorResponse springError,
      Exception exception,
      HttpServletRequest request) {
    HttpStatus status = HttpStatus.resolve(springError.getStatusCode().value());
    if (status == null) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    if (status.is5xxServerError()) {
      log.error(
          "Server error on {} {}",
          request.getMethod(),
          sanitizeForLog(request.getRequestURI()),
          exception);
    } else {
      log.debug(
          "Client error {} on {} {}: {}",
          status.value(),
          request.getMethod(),
          sanitizeForLog(request.getRequestURI()),
          exception.getMessage());
    }

    String detail = springError.getBody().getDetail();
    String message = detail != null && !detail.isBlank() ? detail : status.getReasonPhrase();
    ErrorResponse errorResponse =
        buildErrorResponse(status, sanitize(message), sanitize(request.getRequestURI()), null);

    return ResponseEntity.status(status).headers(springError.getHeaders()).body(errorResponse);
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

  /** Strips CR/LF so request-controlled values cannot forge log lines. */
  private String sanitizeForLog(String value) {
    return value == null ? null : value.replaceAll("[\\r\\n]", "_");
  }

  private String sanitize(String value) {
    if (value == null) {
      return null;
    }

    return HtmlUtils.htmlEscape(value);
  }
}
