package com.brewdeck.brewdeck_api.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.reset.PasswordResetMailPort;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitRule;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Auth endpoints throttle per client IP (filter) and per account (services), answering 429 with
 * Retry-After (audit finding 4). Each test uses its own client IPs so buckets never collide.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "brewdeck.rate-limit.enabled=true")
class RateLimitIntegrationTest extends PostgresIntegrationTest {

  private static final AtomicInteger NEXT_IP = new AtomicInteger(1);

  @Autowired private MockMvc mockMvc;
  @MockitoSpyBean private PasswordResetMailPort mailPort;

  private static String freshIp() {
    int n = NEXT_IP.getAndIncrement();
    return "10.42." + (n / 250) + "." + (n % 250 + 1);
  }

  private static RequestPostProcessor from(String ip) {
    return request -> {
      request.setRemoteAddr(ip);
      return request;
    };
  }

  private ResultActions post(String path, String ip, String body) throws Exception {
    return mockMvc.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
            .with(from(ip))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private static String loginBody(String email) {
    return "{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}";
  }

  @Test
  void login_perIpLimit_returns429WithRetryAfterAndLetsOtherIpsThrough() throws Exception {
    String ip = freshIp();
    for (int i = 0; i < RateLimitRule.LOGIN_IP.limit(); i++) {
      // Different emails each time so only the IP bucket fills up.
      post("/api/auth/login", ip, loginBody("ip-" + i + "-" + System.nanoTime() + "@example.com"))
          .andExpect(status().isUnauthorized());
    }

    post("/api/auth/login", ip, loginBody("one-more-" + System.nanoTime() + "@example.com"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(jsonPath("$.message").value("Too many attempts. Try again in a minute."));

    post("/api/auth/login", freshIp(), loginBody("other-ip-" + System.nanoTime() + "@example.com"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_perAccountLimit_holdsEvenWhenTheAttackerRotatesIps() throws Exception {
    String victim = "victim-" + System.nanoTime() + "@example.com";
    for (int i = 0; i < RateLimitRule.LOGIN_EMAIL.limit(); i++) {
      post("/api/auth/login", freshIp(), loginBody(victim)).andExpect(status().isUnauthorized());
    }

    post("/api/auth/login", freshIp(), loginBody(victim))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));
    // Case variants are the same account.
    post("/api/auth/login", freshIp(), loginBody(victim.toUpperCase()))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void forgotPassword_perAccountLimit_capsResetEmails() throws Exception {
    String email = "flood-" + System.nanoTime() + "@example.com";
    post(
            "/api/auth/register",
            freshIp(),
            "{\"email\":\"" + email + "\",\"password\":\"password123\"}")
        .andExpect(status().isCreated());

    for (int i = 0; i < RateLimitRule.FORGOT_PASSWORD_EMAIL.limit(); i++) {
      post("/api/auth/forgot-password", freshIp(), "{\"email\":\"" + email + "\"}")
          .andExpect(status().isOk());
    }
    post("/api/auth/forgot-password", freshIp(), "{\"email\":\"" + email + "\"}")
        .andExpect(status().isTooManyRequests());

    verify(mailPort, times(RateLimitRule.FORGOT_PASSWORD_EMAIL.limit()))
        .sendResetLink(org.mockito.ArgumentMatchers.eq(email), anyString(), any());
  }

  @Test
  void forgotPassword_unknownEmailIsThrottledTheSameWay_noEnumeration() throws Exception {
    String unknown = "nobody-" + System.nanoTime() + "@example.com";
    for (int i = 0; i < RateLimitRule.FORGOT_PASSWORD_EMAIL.limit(); i++) {
      post("/api/auth/forgot-password", freshIp(), "{\"email\":\"" + unknown + "\"}")
          .andExpect(status().isOk());
    }

    post("/api/auth/forgot-password", freshIp(), "{\"email\":\"" + unknown + "\"}")
        .andExpect(status().isTooManyRequests());
    verify(mailPort, never())
        .sendResetLink(org.mockito.ArgumentMatchers.eq(unknown), anyString(), any());
  }

  @Test
  void register_perIpLimit() throws Exception {
    String ip = freshIp();
    for (int i = 0; i < RateLimitRule.REGISTER_IP.limit(); i++) {
      post(
              "/api/auth/register",
              ip,
              "{\"email\":\"reg-"
                  + i
                  + "-"
                  + System.nanoTime()
                  + "@example.com\","
                  + "\"password\":\"password123\"}")
          .andExpect(status().isCreated());
    }

    post(
            "/api/auth/register",
            ip,
            "{\"email\":\"reg-extra-"
                + System.nanoTime()
                + "@example.com\","
                + "\"password\":\"password123\"}")
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void nonAuthEndpoints_areNotThrottledByTheAuthFilter() throws Exception {
    String ip = freshIp();
    for (int i = 0; i < RateLimitRule.LOGIN_IP.limit() + 5; i++) {
      mockMvc
          .perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                      "/api/coffees")
                  .with(from(ip)))
          .andExpect(status().isUnauthorized());
    }
  }
}
