package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitRule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Behind the Next.js proxy (ADR-013) every request reaches the API from the proxy's address. With
 * FORWARD_HEADERS_STRATEGY=native (for deployments where a load balancer in front of Next sets
 * X-Forwarded-For), Tomcat trusts that header from the internal proxy, so per-IP rate limits apply
 * per real client instead of lumping every user together. Uses a real HTTP server: MockMvc bypasses
 * Tomcat's RemoteIpValve.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(
    properties = {"brewdeck.rate-limit.enabled=true", "server.forward-headers-strategy=native"})
class ForwardedClientIpIntegrationTest extends PostgresIntegrationTest {

  @LocalServerPort private int port;

  private final HttpClient http = HttpClient.newHttpClient();

  private int loginFrom(String forwardedFor) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
            .header("Content-Type", "application/json")
            .header("X-Forwarded-For", forwardedFor)
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "{\"email\":\"fwd-"
                        + System.nanoTime()
                        + "@example.com\",\"password\":\"wrong-password\"}"))
            .build();
    return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
  }

  @Test
  void perIpLimit_followsTheForwardedClientAddress_fromTheTrustedLocalProxy() throws Exception {
    String clientA = "203.0.113.10";
    for (int i = 0; i < RateLimitRule.LOGIN_IP.limit(); i++) {
      assertThat(loginFrom(clientA)).isEqualTo(401);
    }

    assertThat(loginFrom(clientA)).as("client A is throttled").isEqualTo(429);
    // Same proxy (localhost), different real client: not throttled.
    assertThat(loginFrom("203.0.113.20")).as("client B is unaffected").isEqualTo(401);
  }
}
