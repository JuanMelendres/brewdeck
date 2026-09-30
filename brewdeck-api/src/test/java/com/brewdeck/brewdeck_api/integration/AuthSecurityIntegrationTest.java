package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
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
  void authResponsesNeverExposeTheRefreshTokenToScripts() throws Exception {
    String email = "no-body-token-" + System.nanoTime() + "@example.com";
    String registered = registerAndRead(email, "password123");
    String loggedIn =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType("application/json")
                    .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    for (String body : new String[] {registered, loggedIn}) {
      assertThat(body).contains("\"token\"").doesNotContain("refreshToken");
    }
  }

  @Test
  void changePasswordRevokesEveryExistingRefreshToken() throws Exception {
    String email = "pw-change-revoke-" + System.nanoTime() + "@example.com";
    MockHttpServletResponse registered = register(email, "password123");
    String access = com.jayway.jsonpath.JsonPath.read(registered.getContentAsString(), "$.token");
    Cookie fromRegister = registered.getCookie(REFRESH_COOKIE);
    // A second session (e.g. another device) holding its own refresh cookie.
    Cookie fromLogin = loginCookie(email, "password123");

    mockMvc
        .perform(
            post("/api/auth/change-password")
                .header("Authorization", "Bearer " + access)
                .contentType("application/json")
                .content("{\"currentPassword\":\"password123\",\"newPassword\":\"newpassword1\"}"))
        .andExpect(status().isNoContent());

    // Every session issued under the old password is dead.
    for (Cookie stale : new Cookie[] {fromRegister, fromLogin}) {
      refreshWith(stale).andExpect(status().isUnauthorized());
    }

    // A fresh login with the new password yields a working refresh cookie.
    refreshWith(loginCookie(email, "newpassword1")).andExpect(status().isOk());
  }

  @Test
  void logoutRevokesThePresentedRefreshCookie() throws Exception {
    String email = "logout-flow-" + System.nanoTime() + "@example.com";
    MockHttpServletResponse registered = register(email, "password123");
    String access = com.jayway.jsonpath.JsonPath.read(registered.getContentAsString(), "$.token");
    Cookie refresh = registered.getCookie(REFRESH_COOKIE);

    mockMvc
        .perform(
            post("/api/auth/logout")
                .header("Authorization", "Bearer " + access)
                .header("X-Requested-With", "fetch")
                .cookie(refresh))
        .andExpect(status().isNoContent());

    // Even if a client kept the old cookie value, it can no longer be rotated.
    refreshWith(refresh).andExpect(status().isUnauthorized());
  }

  @Test
  void refreshTokenInTheBody_isNoLongerAccepted() throws Exception {
    String email = "body-token-" + System.nanoTime() + "@example.com";
    String rawToken = register(email, "password123").getCookie(REFRESH_COOKIE).getValue();

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .header("X-Requested-With", "fetch")
                .contentType("application/json")
                .content("{\"refreshToken\":\"" + rawToken + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  private static final String REFRESH_COOKIE = "brewdeck_refresh";

  private MockHttpServletResponse register(String email, String password) throws Exception {
    return mockMvc
        .perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse();
  }

  private Cookie loginCookie(String email, String password) throws Exception {
    return mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getCookie(REFRESH_COOKIE);
  }

  private org.springframework.test.web.servlet.ResultActions refreshWith(Cookie cookie)
      throws Exception {
    return mockMvc.perform(
        post("/api/auth/refresh").cookie(cookie).header("X-Requested-With", "fetch"));
  }

  private String registerAndRead(String email, String password) throws Exception {
    return register(email, password).getContentAsString();
  }

  @Test
  void refreshCookieFlow_rotatesOnRefresh_detectsReuse_andIsClearedOnLogout() throws Exception {
    String email = "cookie-flow-" + System.nanoTime() + "@example.com";
    var login =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType("application/json")
                    .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    jakarta.servlet.http.Cookie first = login.getCookie("brewdeck_refresh");
    assertThat(first).isNotNull();
    assertThat(first.isHttpOnly()).isTrue();
    assertThat(first.getSecure()).isTrue();
    assertThat(first.getPath()).isEqualTo("/api/auth");

    var rotated =
        mockMvc
            .perform(post("/api/auth/refresh").cookie(first).header("X-Requested-With", "fetch"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn()
            .getResponse();
    jakarta.servlet.http.Cookie second = rotated.getCookie("brewdeck_refresh");
    assertThat(second.getValue()).isNotEqualTo(first.getValue());
    String access = com.jayway.jsonpath.JsonPath.read(rotated.getContentAsString(), "$.token");

    // Replaying the old cookie is reuse: 401, and it revokes the rotated one too.
    mockMvc
        .perform(post("/api/auth/refresh").cookie(first).header("X-Requested-With", "fetch"))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(post("/api/auth/refresh").cookie(second).header("X-Requested-With", "fetch"))
        .andExpect(status().isUnauthorized());

    // Logout clears the cookie.
    var logout =
        mockMvc
            .perform(
                post("/api/auth/logout")
                    .header("Authorization", "Bearer " + access)
                    .header("X-Requested-With", "fetch"))
            .andExpect(status().isNoContent())
            .andReturn()
            .getResponse();
    assertThat(logout.getCookie("brewdeck_refresh").getMaxAge()).isZero();
  }

  @Test
  void refreshWithCookieButWithoutCsrfHeader_isForbidden() throws Exception {
    String email = "cookie-csrf-" + System.nanoTime() + "@example.com";
    jakarta.servlet.http.Cookie cookie =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType("application/json")
                    .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
            .andReturn()
            .getResponse()
            .getCookie("brewdeck_refresh");

    mockMvc.perform(post("/api/auth/refresh").cookie(cookie)).andExpect(status().isForbidden());
  }
}
