package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Actuator on its own port (OBS-01, ADR-023): Prometheus answers there without a token and never on
 * the main port, which Cloud Run exposes; the public health check stays on the main port.
 */
// Tests turn metric exporters off by default; this keeps the Prometheus registry.
@AutoConfigureMetrics
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "management.server.port=0")
@ActiveProfiles("test")
class ManagementPortIntegrationTest extends PostgresIntegrationTest {

  private final HttpClient client = HttpClient.newHttpClient();

  @LocalServerPort private int serverPort;
  @LocalManagementPort private int managementPort;

  private HttpResponse<String> get(int port, String path) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void prometheus_isOnTheManagementPort_withCommonTags() throws Exception {
    get(serverPort, "/api/public/legal");

    HttpResponse<String> response = get(managementPort, "/actuator/prometheus");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body())
        .contains("http_server_requests_seconds_bucket")
        .contains("application=\"brewdeck-api\"")
        .contains("environment=\"test\"")
        .contains("uri=\"/api/public/legal\"");
  }

  @Test
  void actuator_isClosedOnTheMainPort() throws Exception {
    assertThat(get(serverPort, "/actuator/prometheus").statusCode()).isEqualTo(401);
    assertThat(get(serverPort, "/actuator/health").statusCode()).isEqualTo(401);
  }

  @Test
  void publicHealth_isOnTheMainPort_withoutDetails() throws Exception {
    HttpResponse<String> response = get(serverPort, "/health");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("{\"status\":\"UP\"}");
  }

  @Test
  void managementPort_servesNoApi() throws Exception {
    assertThat(get(managementPort, "/api/public/legal").statusCode()).isEqualTo(404);
  }
}
