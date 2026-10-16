package com.brewdeck.brewdeck_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Signup no longer returns a session (it answers 202 alike for new and existing emails, AUTH-05.7),
 * so tests that need a logged-in user register and then log in.
 */
final class TestAccounts {

  private TestAccounts() {}

  /**
   * Registers with {@code credentialsJson} ({@code {"email":..,"password":..}}) and logs in with
   * the same body. Returns the login response: {@code $.token} in the body, refresh cookie set.
   */
  static MockHttpServletResponse registerAndLogin(MockMvc mockMvc, String credentialsJson)
      throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentialsJson))
        .andExpect(status().isAccepted());
    return mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentialsJson))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();
  }
}
