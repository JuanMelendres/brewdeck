package com.brewdeck.brewdeck_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** The saved UI language preference (ADR-015). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LanguagePreferenceIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private String register() throws Exception {
    String email = "language-" + System.nanoTime() + "@example.com";
    String response =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + JsonPath.read(response, "$.token");
  }

  private org.springframework.test.web.servlet.ResultActions putLanguage(
      String bearer, String language) throws Exception {
    return mockMvc.perform(
        put("/api/auth/me/language")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"language\":\"" + language + "\"}"));
  }

  @Test
  void newAccount_hasNoLanguageYet() throws Exception {
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", register()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.language").doesNotExist());
  }

  @Test
  void language_isSavedAndReturnedByMe() throws Exception {
    String bearer = register();

    putLanguage(bearer, "ES")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.language").value("ES"));
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", bearer))
        .andExpect(jsonPath("$.language").value("ES"));
  }

  @Test
  void languageWithoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            put("/api/auth/me/language")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"language\":\"ES\"}"))
        .andExpect(status().isUnauthorized());
  }
}
