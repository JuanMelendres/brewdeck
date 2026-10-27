package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * OTLP push export (OBS-01 PR 3, ADR-023): with export on, the API posts gzip'd protobuf metrics to
 * the configured URL with the Authorization header, as Grafana Cloud expects.
 */
@AutoConfigureMetrics
// Tests disable every exporter unless spring.test.metrics.export is true.
@SpringBootTest(properties = "spring.test.metrics.export=true")
@ActiveProfiles("test")
class OtlpExportIntegrationTest extends PostgresIntegrationTest {

  private record Push(String path, String authorization, String contentType, String encoding) {}

  private static final BlockingQueue<Push> PUSHES = new LinkedBlockingQueue<>();
  private static final HttpServer RECEIVER = startReceiver();

  private static HttpServer startReceiver() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            exchange.getRequestBody().readAllBytes();
            PUSHES.add(
                new Push(
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    exchange.getRequestHeaders().getFirst("Content-Encoding")));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @DynamicPropertySource
  static void otlp(DynamicPropertyRegistry registry) {
    registry.add("management.otlp.metrics.export.enabled", () -> "true");
    registry.add(
        "management.otlp.metrics.export.url",
        () -> "http://localhost:" + RECEIVER.getAddress().getPort() + "/otlp/v1/metrics");
    registry.add("management.otlp.metrics.export.headers.Authorization", () -> "Basic dGVzdA==");
    registry.add("management.otlp.metrics.export.step", () -> "1s");
  }

  @AfterAll
  static void stopReceiver() {
    RECEIVER.stop(0);
  }

  @Test
  void pushesMetricsWithTheAuthorizationHeader() throws Exception {
    Push push = PUSHES.poll(15, TimeUnit.SECONDS);

    assertThat(push).isNotNull();
    assertThat(push.path()).isEqualTo("/otlp/v1/metrics");
    assertThat(push.authorization()).isEqualTo("Basic dGVzdA==");
    assertThat(push.contentType()).isEqualTo("application/x-protobuf");
    assertThat(push.encoding()).isEqualTo("gzip");
  }
}
