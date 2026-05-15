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

import com.brewdeck.brewdeck_api.common.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    BrewSessionFilter filter = new BrewSessionFilter(null, null);

    PageResponse<BrewSessionResponse> pageResponse =
        PageResponse.fromPage(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

    when(brewSessionService.search(eq(filter), any(Pageable.class))).thenReturn(pageResponse);

    mockMvc
        .perform(get("/api/brew-sessions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(1L))
        .andExpect(jsonPath("$.content[0].recipeName").value("Mezcla Veracruz AeroPress"))
        .andExpect(jsonPath("$.content[0].rating").value(9))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(brewSessionService).search(eq(filter), any(Pageable.class));
  }

  @Test
  void findAll_shouldReturnFilteredBrewSessions() throws Exception {
    BrewSessionResponse response = buildBrewSessionResponse();
    BrewSessionFilter filter = new BrewSessionFilter(1L, 9);

    PageResponse<BrewSessionResponse> pageResponse =
        PageResponse.fromPage(new PageImpl<>(List.of(response), PageRequest.of(0, 5), 1));

    when(brewSessionService.search(eq(filter), any(Pageable.class))).thenReturn(pageResponse);

    mockMvc
        .perform(
            get("/api/brew-sessions")
                .param("recipeId", "1")
                .param("rating", "9")
                .param("page", "0")
                .param("size", "5")
                .param("sort", "id,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].recipeId").value(1L))
        .andExpect(jsonPath("$.content[0].rating").value(9))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(brewSessionService).search(eq(filter), any(Pageable.class));
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

    PageResponse<BrewSessionResponse> pageResponse =
        PageResponse.fromPage(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

    when(brewSessionService.findByRecipeId(eq(1L), any(Pageable.class))).thenReturn(pageResponse);

    mockMvc
        .perform(get("/api/brew-sessions/recipe/{recipeId}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].recipeId").value(1L))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(brewSessionService).findByRecipeId(eq(1L), any(Pageable.class));
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
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.rating").value(9));

    verify(brewSessionService).create(any(BrewSessionRequest.class));
  }

  @Test
  void create_shouldReturnValidationErrorBody_whenRecipeIdIsMissing() throws Exception {
    BrewSessionRequest request =
        new BrewSessionRequest(null, "Timemore S3 - 5.5", 90, "2:30", "Balanced", 9, "Repeat.");

    mockMvc
        .perform(
            post("/api/brew-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.recipeId").value("Recipe id is required"));
  }

  @Test
  void create_shouldReturnBadRequest_whenRatingIsTooHigh() throws Exception {
    BrewSessionRequest request =
        new BrewSessionRequest(1L, "Timemore S3 - 5.5", 90, "2:30", "Balanced", 11, "Repeat.");

    mockMvc
        .perform(
            post("/api/brew-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.rating").value("Rating must not exceed 10"));
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

    mockMvc.perform(delete("/api/brew-sessions/{id}", 1L)).andExpect(status().isNoContent());

    verify(brewSessionService).delete(1L);
  }

  @Test
  void create_shouldReturnBadRequest_whenRatingIsTooLow() throws Exception {
    BrewSessionRequest request =
        new BrewSessionRequest(1L, "Timemore S3 - 5.5", 90, "2:30", "Balanced", 0, "Repeat.");

    mockMvc
        .perform(
            post("/api/brew-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.rating").value("Rating must be at least 1"));
  }

  @Test
  void create_shouldReturnBadRequest_whenActualTempIsTooLow() throws Exception {
    BrewSessionRequest request =
        new BrewSessionRequest(1L, "Timemore S3 - 5.5", 69, "2:30", "Balanced", 9, "Repeat.");

    mockMvc
        .perform(
            post("/api/brew-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.actualTemp")
                .value("Actual temperature must be at least 70 degrees Celsius"));
  }

  @Test
  void create_shouldReturnBadRequest_whenActualTempIsTooHigh() throws Exception {
    BrewSessionRequest request =
        new BrewSessionRequest(1L, "Timemore S3 - 5.5", 101, "2:30", "Balanced", 9, "Repeat.");

    mockMvc
        .perform(
            post("/api/brew-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.actualTemp")
                .value("Actual temperature must not exceed 100 degrees Celsius"));
  }

  @Test
  void create_shouldReturnBadRequest_whenActualGrindExceedsMaxLength() throws Exception {
    BrewSessionRequest request =
        new BrewSessionRequest(1L, "A".repeat(121), 90, "2:30", "Balanced", 9, "Repeat.");

    mockMvc
        .perform(
            post("/api/brew-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.actualGrind")
                .value("Actual grind must not exceed 120 characters"));
  }

  @Test
  void create_shouldReturnBadRequest_whenTasteResultExceedsMaxLength() throws Exception {
    BrewSessionRequest request =
        new BrewSessionRequest(1L, "Timemore S3 - 5.5", 90, "2:30", "A".repeat(1001), 9, "Repeat.");

    mockMvc
        .perform(
            post("/api/brew-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.tasteResult")
                .value("Taste result must not exceed 1000 characters"));
  }

  @Test
  void create_shouldReturnBadRequest_whenAdjustmentNotesExceedsMaxLength() throws Exception {
    BrewSessionRequest request =
        new BrewSessionRequest(
            1L, "Timemore S3 - 5.5", 90, "2:30", "Balanced", 9, "A".repeat(1001));

    mockMvc
        .perform(
            post("/api/brew-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.adjustmentNotes")
                .value("Adjustment notes must not exceed 1000 characters"));
  }

  @Test
  void update_shouldReturnBadRequest_whenRatingIsTooHigh() throws Exception {
    BrewSessionRequest request =
        new BrewSessionRequest(1L, "Timemore S3 - 5.5", 90, "2:30", "Balanced", 11, "Repeat.");

    mockMvc
        .perform(
            put("/api/brew-sessions/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.rating").value("Rating must not exceed 10"));
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
