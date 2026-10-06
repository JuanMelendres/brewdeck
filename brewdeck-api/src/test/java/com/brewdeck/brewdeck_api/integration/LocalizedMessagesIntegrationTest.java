package com.brewdeck.brewdeck_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Error and validation messages follow {@code Accept-Language}, limited to English and Spanish, and
 * accents survive the response sanitizing (ADR-015).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class LocalizedMessagesIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static MockHttpServletRequestBuilder blankCoffee() {
    return post("/api/coffees").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}");
  }

  @Test
  void spanishRequest_getsSpanishValidationMessagesWithAccents() throws Exception {
    mockMvc
        .perform(blankCoffee().header(HttpHeaders.ACCEPT_LANGUAGE, "es-MX,es;q=0.9"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("La validación falló"))
        .andExpect(jsonPath("$.validationErrors.name").value("El nombre del café es obligatorio"));
  }

  @Test
  void noLanguageHeader_getsEnglish() throws Exception {
    mockMvc
        .perform(blankCoffee())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.name").value("Coffee name is required"));
  }

  @Test
  void unsupportedLanguage_getsEnglish() throws Exception {
    mockMvc
        .perform(blankCoffee().header(HttpHeaders.ACCEPT_LANGUAGE, "fr-FR"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.name").value("Coffee name is required"));
  }
}
