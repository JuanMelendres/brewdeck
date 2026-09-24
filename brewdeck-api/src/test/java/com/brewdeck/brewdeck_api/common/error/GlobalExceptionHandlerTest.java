package com.brewdeck.brewdeck_api.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  private MockHttpServletRequest request(String method, String uri) {
    return new MockHttpServletRequest(method, uri);
  }

  @Test
  void unexpectedException_returnsGeneric500AndLogsTheStackTrace(CapturedOutput output) {
    ResponseEntity<ErrorResponse> response =
        handler.handleGenericException(
            new IllegalStateException("boom-internal-detail"), request("GET", "/api/coffees"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    // The client never sees internals...
    assertThat(response.getBody().message()).isEqualTo("Unexpected error occurred");
    // ...but operators do.
    assertThat(output)
        .contains("Unhandled exception on GET /api/coffees")
        .contains("IllegalStateException: boom-internal-detail");
  }

  @Test
  void springWebError_keepsItsStatusAndHeaders() {
    ResponseEntity<ErrorResponse> response =
        handler.handleGenericException(
            new HttpRequestMethodNotSupportedException("DELETE", java.util.List.of("GET", "POST")),
            request("DELETE", "/api/coffees"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getHeaders().getAllow()).contains(HttpMethod.GET, HttpMethod.POST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(405);
  }

  @Test
  void responseStatusException_withoutReason_usesReasonPhrase() {
    ResponseEntity<ErrorResponse> response =
        handler.handleGenericException(
            new ResponseStatusException(HttpStatus.CONFLICT), request("POST", "/api/x"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Conflict");
  }

  @Test
  void springWebServerError_isLogged(CapturedOutput output) {
    ResponseEntity<ErrorResponse> response =
        handler.handleGenericException(
            new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "downstream down"),
            request("GET", "/api/y"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(output).contains("Server error on GET /api/y");
  }

  @Test
  void logLines_cannotBeForgedThroughTheRequestUri(CapturedOutput output) {
    handler.handleGenericException(
        new IllegalStateException("x"), request("GET", "/api/a\r\nFAKE LOG LINE"));

    assertThat(output).contains("/api/a__FAKE LOG LINE").doesNotContain("\nFAKE LOG LINE");
  }
}
