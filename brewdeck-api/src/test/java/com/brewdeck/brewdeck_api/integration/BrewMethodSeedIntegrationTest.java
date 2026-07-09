package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class BrewMethodSeedIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void findAll_shouldReturnSeededBrewMethods() throws Exception {
    String response =
        mockMvc
            .perform(
                get("/api/brew-methods")
                    .param("page", "0")
                    .param("size", "50")
                    .param("sort", "id,asc"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode page = objectMapper.readTree(response);
    JsonNode methods = page.get("content");

    assertThat(methods).isNotEmpty();
    assertThat(methods.toString()).contains("AeroPress");
    assertThat(methods.toString()).contains("V60");
    assertThat(methods.toString()).contains("Espresso");
    assertThat(page.get("page").asInt()).isZero();
    assertThat(page.get("size").asInt()).isEqualTo(50);
  }
}
