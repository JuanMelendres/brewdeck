package com.brewdeck.brewdeck_api.common.error;

import com.brewdeck.brewdeck_api.ai.AiUnavailableException;
import com.brewdeck.brewdeck_api.ai.InsufficientBrewHistoryException;
import com.brewdeck.brewdeck_api.auth.EmailAlreadyUsedException;
import com.brewdeck.brewdeck_api.auth.InvalidCurrentPasswordException;
import com.brewdeck.brewdeck_api.auth.refresh.InvalidRefreshTokenException;
import com.brewdeck.brewdeck_api.auth.reset.InvalidResetTokenException;
import com.brewdeck.brewdeck_api.auth.verification.InvalidVerificationTokenException;
import com.brewdeck.brewdeck_api.common.i18n.LocaleConfig;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitExceededException;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitMessages;
import com.brewdeck.brewdeck_api.featureflag.FeatureDisabledException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
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

  private final MessageSource messageSource;

  public GlobalExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /** A message from messages*.properties in the request's language (ADR-015). */
  private String message(String key, Object... args) {
    return messageSource.getMessage(key, args, requestLocale());
  }

  /**
   * The locale the DispatcherServlet resolved for this request. Outside a request there is none,
   * and {@link LocaleContextHolder#getLocale()} would fall back to the JVM's locale, so use
   * English.
   */
  private static Locale requestLocale() {
    return LocaleContextHolder.getLocaleContext() == null
        ? LocaleConfig.DEFAULT_LOCALE
        : LocaleContextHolder.getLocale();
  }

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
            message("error.validationFailed"),
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
            message("error.malformedBody"),
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
            message("error.invalidParameter", sanitize(exception.getName())),
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
            message("error.invalidSort", sanitize(exception.getPropertyName())),
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
            message("error.badCredentials"),
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
            message("error.invalidRefreshToken"),
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
            message("error.invalidVerificationToken"),
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
            message("error.invalidResetToken"),
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
            message("error.currentPasswordIncorrect"),
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
      RateLimitExceededException exception, HttpServletRequest request) {
    long retryAfterSeconds = exception.getDecision().retryAfterSeconds();
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.TOO_MANY_REQUESTS,
            RateLimitMessages.tooManyAttempts(messageSource, requestLocale(), retryAfterSeconds),
            sanitize(request.getRequestURI()),
            null);

    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds))
        .body(errorResponse);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      AccessDeniedException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.FORBIDDEN,
            message("error.forbidden"),
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

  @ExceptionHandler(ResourceInUseException.class)
  public ResponseEntity<ErrorResponse> handleResourceInUse(
      ResourceInUseException exception, HttpServletRequest request) {
    ErrorResponse errorResponse =
        buildErrorResponse(
            HttpStatus.CONFLICT,
            message(exception.getMessageKey(), exception.getCount()),
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
            message("error.dataIntegrity"),
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
            message("error.aiUnavailable"),
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
            message("error.noRatedHistory"),
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
            ? message("error.featureUnavailable")
            : message("error.featureNotAvailable");
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
            message("error.unexpected"),
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
        Instant.now(), status.value(), status.getReasonPhrase(), message, path, validationErrors);
  }

  /** Strips CR/LF so request-controlled values cannot forge log lines. */
  private String sanitizeForLog(String value) {
    return value == null ? null : value.replaceAll("[\\r\\n]", "_");
  }

  private String sanitize(String value) {
    if (value == null) {
      return null;
    }

    // With an encoding, only markup characters (<>&"') are escaped; accents and symbols such as
    // "é" or "°" stay readable instead of becoming "&eacute;" (Spanish messages, ADR-015).
    return HtmlUtils.htmlEscape(value, "UTF-8");
  }
}
