package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.reset.PasswordResetMailPort;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/** Emails are case-insensitive end to end: one account per address (audit finding 6). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailNormalizationIntegrationTest extends PostgresIntegrationTest {

  private static final String PASSWORD = "password123";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @MockitoSpyBean private PasswordResetMailPort mailPort;

  @Test
  void register_storesTheNormalizedEmail() throws Exception {
    String local = "mixed.case-" + System.nanoTime();

    String response =
        register("  " + local.toUpperCase() + "@Example.COM ")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(local + "@example.com"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String token = JsonPath.read(response, "$.token");
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(local + "@example.com"));
  }

  @Test
  void register_sameAddressInDifferentCase_isAConflict() throws Exception {
    String local = "dup-" + System.nanoTime();
    register(local + "@example.com").andExpect(status().isCreated());

    register(local.toUpperCase() + "@EXAMPLE.com").andExpect(status().isConflict());
  }

  @Test
  void login_isCaseInsensitive() throws Exception {
    String local = "login-" + System.nanoTime();
    register(local + "@example.com").andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\" "
                        + local.toUpperCase()
                        + "@Example.com\",\"password\":\""
                        + PASSWORD
                        + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(local + "@example.com"));
  }

  @Test
  void forgotPassword_findsTheAccountRegardlessOfCase() throws Exception {
    String local = "forgot-" + System.nanoTime();
    register(local + "@example.com").andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + local.toUpperCase() + "@EXAMPLE.COM\"}"))
        .andExpect(status().isOk());

    verify(mailPort).sendResetLink(eq(local + "@example.com"), anyString());
  }

  @Test
  void database_rejectsANonNormalizedEmail() {
    User user =
        User.builder()
            .email("Not.Normalized-" + System.nanoTime() + "@Example.com")
            .passwordHash("hash")
            .createdAt(LocalDateTime.now())
            .build();

    assertThatThrownBy(() -> userRepository.saveAndFlush(user))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private org.springframework.test.web.servlet.ResultActions register(String email)
      throws Exception {
    return mockMvc.perform(
        post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"));
  }
}
