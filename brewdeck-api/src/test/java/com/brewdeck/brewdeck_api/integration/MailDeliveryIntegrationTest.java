package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;

/**
 * End-to-end email delivery through a real SMTP server (Mailpit in Testcontainers): the app sends
 * real messages, and the tokens in their links work. No mocks between the service and the wire.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MailDeliveryIntegrationTest extends PostgresIntegrationTest {

  private static final int SMTP_PORT = 1025;
  private static final int API_PORT = 8025;

  @SuppressWarnings("resource")
  static final GenericContainer<?> MAILPIT =
      new GenericContainer<>("axllent/mailpit:v1.27").withExposedPorts(SMTP_PORT, API_PORT);

  static {
    MAILPIT.start();
  }

  @DynamicPropertySource
  static void mailProperties(DynamicPropertyRegistry registry) {
    registry.add("brewdeck.mail.enabled", () -> "true");
    registry.add("spring.mail.host", MAILPIT::getHost);
    registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(SMTP_PORT));
    registry.add("brewdeck.mail.frontend-base-url", () -> "http://brewdeck.test");
  }

  private static final Pattern TOKEN = Pattern.compile("\\?token=([A-Za-z0-9_\\-]+)");
  private final HttpClient http = HttpClient.newHttpClient();

  @Autowired private MockMvc mockMvc;

  /** Polls Mailpit until a message to {@code recipient} with {@code subject} arrives. */
  private String awaitMessageText(String recipient, String subject) throws Exception {
    String base = "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(API_PORT);
    Instant deadline = Instant.now().plus(Duration.ofSeconds(15));
    while (Instant.now().isBefore(deadline)) {
      String search =
          http.send(
                  HttpRequest.newBuilder(
                          URI.create(
                              base
                                  + "/api/v1/search?query="
                                  + java.net.URLEncoder.encode(
                                      "to:\"" + recipient + "\"",
                                      java.nio.charset.StandardCharsets.UTF_8)))
                      .build(),
                  HttpResponse.BodyHandlers.ofString())
              .body();
      List<String> ids = JsonPath.read(search, "$.messages[?(@.Subject == '" + subject + "')].ID");
      if (!ids.isEmpty()) {
        String message =
            http.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/v1/message/" + ids.get(0)))
                        .build(),
                    HttpResponse.BodyHandlers.ofString())
                .body();
        return JsonPath.read(message, "$.Text");
      }
      Thread.sleep(200);
    }
    throw new AssertionError("No '" + subject + "' email reached " + recipient);
  }

  private static String tokenFrom(String text, String path) {
    assertThat(text).contains("http://brewdeck.test" + path + "?token=");
    Matcher matcher = TOKEN.matcher(text);
    assertThat(matcher.find()).isTrue();
    return matcher.group(1);
  }

  @Test
  void registration_sendsARealVerificationEmail_whoseLinkVerifiesTheAccount() throws Exception {
    String email = "mail-verify-" + System.nanoTime() + "@example.com";
    String registered =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String bearer = "Bearer " + JsonPath.read(registered, "$.token");

    String token =
        tokenFrom(awaitMessageText(email, "Verify your BrewDeck email"), "/verify-email");

    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", bearer))
        .andExpect(jsonPath("$.emailVerified").value(true));
  }

  @Test
  void forgotPassword_sendsARealResetEmail_whoseLinkResetsThePassword() throws Exception {
    String email = "mail-reset-" + System.nanoTime() + "@example.com";
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"))
        .andExpect(status().isOk());
    String token =
        tokenFrom(awaitMessageText(email, "Reset your BrewDeck password"), "/reset-password");

    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"brand-new-pass\"}"))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"brand-new-pass\"}"))
        .andExpect(status().isOk());
  }
}
