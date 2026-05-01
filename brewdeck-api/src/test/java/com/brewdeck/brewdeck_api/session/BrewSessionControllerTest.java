package com.brewdeck.brewdeck_api.session;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BrewSessionController.class)
class BrewSessionControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private BrewSessionService brewSessionService;

  @Test
  void findAll_shouldReturnBrewSessions() throws Exception {
    BrewSessionResponse response = buildBrewSessionResponse();

    when(brewSessionService.findAll()).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/brew-sessions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].recipeName").value("Mezcla Veracruz AeroPress"))
        .andExpect(jsonPath("$[0].rating").value(9));

    verify(brewSessionService).findAll();
  }

  @Test
  void findById_shouldReturnBrewSession() throws Exception {
    BrewSessionResponse response = buildBrewSessionResponse();

    when(brewSessionService.findById(1L)).thenReturn(response);

    mockMvc
        .perform(get("/api/brew-sessions/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.recipeId").value(1L))
        .andExpect(jsonPath("$.rating").value(9));

    verify(brewSessionService).findById(1L);
  }

  @Test
  void findByRecipeId_shouldReturnBrewSessions() throws Exception {
    BrewSessionResponse response = buildBrewSessionResponse();

    when(brewSessionService.findByRecipeId(1L)).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/brew-sessions/recipe/{recipeId}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].recipeId").value(1L));

    verify(brewSessionService).findByRecipeId(1L);
  }

  @Test
  void create_shouldReturnCreatedBrewSession() throws Exception {
    BrewSessionRequest request = buildBrewSessionRequest();
    BrewSessionResponse response = buildBrewSessionResponse();

    when(brewSessionService.create(any(BrewSessionRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/brew-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.rating").value(9));

    verify(brewSessionService).create(any(BrewSessionRequest.class));
  }

  @Test
  void create_shouldReturnBadRequest_whenRecipeIdIsMissing() throws Exception {
    BrewSessionRequest request =
        new BrewSessionRequest(null, "Timemore S3 - 5.5", 90, "2:30", "Balanced", 9, "Repeat.");

    mockMvc
        .perform(
            post("/api/brew-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void create_shouldReturnBadRequest_whenRatingIsInvalid() throws Exception {
    String requestBody =
        """
            {
              "recipeId": 1,
              "actualGrind": "Timemore S3 - 5.5",
              "actualTemp": 90,
              "actualTime": "2:30",
              "tasteResult": "Balanced",
              "rating": 11,
              "adjustmentNotes": "Repeat."
            }
            """;

    mockMvc
        .perform(
            post("/api/brew-sessions").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void update_shouldReturnUpdatedBrewSession() throws Exception {
    BrewSessionRequest request = buildBrewSessionRequest();
    BrewSessionResponse response = buildBrewSessionResponse();

    when(brewSessionService.update(eq(1L), any(BrewSessionRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            put("/api/brew-sessions/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L));

    verify(brewSessionService).update(eq(1L), any(BrewSessionRequest.class));
  }

  @Test
  void delete_shouldDeleteBrewSession() throws Exception {
    doNothing().when(brewSessionService).delete(1L);

    mockMvc.perform(delete("/api/brew-sessions/{id}", 1L)).andExpect(status().isOk());

    verify(brewSessionService).delete(1L);
  }

  private BrewSessionRequest buildBrewSessionRequest() {
    return new BrewSessionRequest(
        1L,
        "Timemore S3 - 5.5",
        90,
        "2:30",
        "Balanced, clean and aromatic.",
        9,
        "Repeat same recipe.");
  }

  private BrewSessionResponse buildBrewSessionResponse() {
    return new BrewSessionResponse(
        1L,
        1L,
        "Mezcla Veracruz AeroPress",
        LocalDateTime.now(),
        "Timemore S3 - 5.5",
        90,
        "2:30",
        "Balanced, clean and aromatic.",
        9,
        "Repeat same recipe.");
  }
}
