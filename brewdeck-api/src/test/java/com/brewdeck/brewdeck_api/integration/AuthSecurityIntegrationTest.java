package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void protectedEndpoint_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/coffees").param("page", "0").param("size", "10").param("sort", "id,asc"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void publicShareEndpoint_withoutToken_isReachable() throws Exception {
    // Unknown token -> 404 (reachable, i.e. not blocked by 401).
    mockMvc.perform(get("/api/public/recipes/unknown-token")).andExpect(status().isNotFound());
  }

  @Test
  void registerThenLoginThenCallProtected_succeeds() throws Exception {
    String email = "flow-" + System.nanoTime() + "@example.com";
    String body = "{\"email\":\"" + email + "\",\"password\":\"password1\"}";

    String registerResponse =
        mockMvc
            .perform(post("/api/auth/register").contentType("application/json").content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String token = com.jayway.jsonpath.JsonPath.read(registerResponse, "$.token");

    mockMvc
        .perform(
            get("/api/coffees")
                .header("Authorization", "Bearer " + token)
                .param("page", "0")
                .param("size", "10")
                .param("sort", "id,asc"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email));
  }

  @Test
  void me_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void updateProfileThenChangePassword_persistsAndRelogsIn() throws Exception {
    String email = "profile-" + System.nanoTime() + "@example.com";
    String register = "{\"email\":\"" + email + "\",\"password\":\"password1\"}";
    String token =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(
                    post("/api/auth/register").contentType("application/json").content(register))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.token");

    mockMvc
        .perform(
            patch("/api/auth/me")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"displayName\":\"Barista Bob\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Barista Bob"));

    // Persisted: a fresh /me read reflects the new name.
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Barista Bob"));

    // Wrong current password is rejected.
    mockMvc
        .perform(
            post("/api/auth/change-password")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"newpassword1\"}"))
        .andExpect(status().isBadRequest());

    // Correct current password succeeds.
    mockMvc
        .perform(
            post("/api/auth/change-password")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"currentPassword\":\"password1\",\"newPassword\":\"newpassword1\"}"))
        .andExpect(status().isNoContent());

    // New password logs in; old one no longer does.
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"newpassword1\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"password1\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refreshRotatesAndReuseOfOldTokenRevokesTheChain() throws Exception {
    // register a fresh user and capture both tokens from the response body
    String email = "refresh-flow-" + System.nanoTime() + "@example.com";
    String registered = registerAndRead(email, "password123");
    String refresh1 = com.jayway.jsonpath.JsonPath.read(registered, "$.refreshToken");

    // 1) rotate: refresh1 -> (access2, refresh2)
    String rotated = refreshAndRead(refresh1);
    String refresh2 = com.jayway.jsonpath.JsonPath.read(rotated, "$.refreshToken");
    assertThat(refresh2).isNotEqualTo(refresh1);

    // 2) the OLD refresh (refresh1) is now used -> presenting it again is 401 (reuse)
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType("application/json")
                .content("{\"refreshToken\":\"" + refresh1 + "\"}"))
        .andExpect(status().isUnauthorized());

    // 3) reuse revoked the whole active set, so refresh2 (issued during the rotation) is dead too
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType("application/json")
                .content("{\"refreshToken\":\"" + refresh2 + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutRevokesThePresentedRefreshToken() throws Exception {
    String email = "logout-flow-" + System.nanoTime() + "@example.com";
    String registered = registerAndRead(email, "password123");
    String access = com.jayway.jsonpath.JsonPath.read(registered, "$.token");
    String refresh = com.jayway.jsonpath.JsonPath.read(registered, "$.refreshToken");

    // logout requires a valid access token (authenticated endpoint)
    mockMvc
        .perform(
            post("/api/auth/logout")
                .header("Authorization", "Bearer " + access)
                .contentType("application/json")
                .content("{\"refreshToken\":\"" + refresh + "\"}"))
        .andExpect(status().isNoContent());

    // the logged-out refresh token can no longer be rotated
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType("application/json")
                .content("{\"refreshToken\":\"" + refresh + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  private String registerAndRead(String email, String password) throws Exception {
    String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    return mockMvc
        .perform(post("/api/auth/register").contentType("application/json").content(body))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String refreshAndRead(String refreshToken) throws Exception {
    String body = "{\"refreshToken\":\"" + refreshToken + "\"}";
    return mockMvc
        .perform(post("/api/auth/refresh").contentType("application/json").content(body))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }
}
