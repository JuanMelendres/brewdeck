package com.brewdeck.brewdeck_api.method;

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

import com.brewdeck.brewdeck_api.common.pagination.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BrewMethodController.class)
class BrewMethodControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private BrewMethodService brewMethodService;

  @Test
  void getUsage_shouldReturnUsageBreakdown() throws Exception {
    when(brewMethodService.getUsage())
        .thenReturn(
            List.of(
                new MethodUsageResponse(1L, "AeroPress", 5L),
                new MethodUsageResponse(2L, "V60", 0L)));

    mockMvc
        .perform(get("/api/brew-methods/usage"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].methodId").value(1L))
        .andExpect(jsonPath("$[0].methodName").value("AeroPress"))
        .andExpect(jsonPath("$[0].recipeCount").value(5))
        .andExpect(jsonPath("$[1].recipeCount").value(0));

    verify(brewMethodService).getUsage();
  }

  @Test
  void findAll_shouldReturnBrewMethods() throws Exception {
    BrewMethodResponse response =
        new BrewMethodResponse(
            1L, "AeroPress", "Immersion and pressure-based brewing method.", LocalDateTime.now());

    PageResponse<BrewMethodResponse> pageResponse =
        PageResponse.fromPage(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

    when(brewMethodService.findAll(any(Pageable.class))).thenReturn(pageResponse);

    mockMvc
        .perform(get("/api/brew-methods"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(1L))
        .andExpect(jsonPath("$.content[0].name").value("AeroPress"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.first").value(true))
        .andExpect(jsonPath("$.last").value(true));

    verify(brewMethodService).findAll(any(Pageable.class));
  }

  @Test
  void findAll_shouldReturnPagedBrewMethods() throws Exception {
    BrewMethodResponse response =
        new BrewMethodResponse(1L, "V60", "Pour-over brewing method.", LocalDateTime.now());

    PageResponse<BrewMethodResponse> pageResponse =
        PageResponse.fromPage(new PageImpl<>(List.of(response), PageRequest.of(0, 5), 1));

    when(brewMethodService.findAll(any(Pageable.class))).thenReturn(pageResponse);

    mockMvc
        .perform(
            get("/api/brew-methods").param("page", "0").param("size", "5").param("sort", "id,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("V60"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(brewMethodService).findAll(any(Pageable.class));
  }

  @Test
  void findById_shouldReturnBrewMethod() throws Exception {
    BrewMethodResponse response =
        new BrewMethodResponse(
            1L, "AeroPress", "Immersion and pressure-based brewing method.", LocalDateTime.now());

    when(brewMethodService.findById(1L)).thenReturn(response);

    mockMvc
        .perform(get("/api/brew-methods/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("AeroPress"));

    verify(brewMethodService).findById(1L);
  }

  @Test
  void create_shouldReturnCreatedBrewMethod() throws Exception {
    BrewMethodRequest request =
        new BrewMethodRequest("AeroPress", "Immersion and pressure-based brewing method.");

    BrewMethodResponse response =
        new BrewMethodResponse(1L, request.name(), request.description(), LocalDateTime.now());

    when(brewMethodService.create(any(BrewMethodRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/brew-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("AeroPress"));

    verify(brewMethodService).create(any(BrewMethodRequest.class));
  }

  @Test
  void create_shouldReturnValidationErrorBody_whenNameIsBlank() throws Exception {
    BrewMethodRequest request =
        new BrewMethodRequest("", "Immersion and pressure-based brewing method.");

    mockMvc
        .perform(
            post("/api/brew-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.name").value("Method name is required"));
  }

  @Test
  void create_shouldReturnConflict_whenBrewMethodNameIsDuplicated() throws Exception {
    BrewMethodRequest request = new BrewMethodRequest("AeroPress", "Duplicated method.");

    when(brewMethodService.create(any(BrewMethodRequest.class)))
        .thenThrow(new DataIntegrityViolationException("Duplicate key"));

    mockMvc
        .perform(
            post("/api/brew-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.error").value("Conflict"))
        .andExpect(jsonPath("$.message").value("Data integrity violation"));

    verify(brewMethodService).create(any(BrewMethodRequest.class));
  }

  @Test
  void update_shouldReturnUpdatedBrewMethod() throws Exception {
    BrewMethodRequest request = new BrewMethodRequest("V60", "Pour-over brewing method.");

    BrewMethodResponse response =
        new BrewMethodResponse(1L, request.name(), request.description(), LocalDateTime.now());

    when(brewMethodService.update(eq(1L), any(BrewMethodRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            put("/api/brew-methods/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("V60"));

    verify(brewMethodService).update(eq(1L), any(BrewMethodRequest.class));
  }

  @Test
  void delete_shouldDeleteBrewMethod() throws Exception {
    doNothing().when(brewMethodService).delete(1L);

    mockMvc.perform(delete("/api/brew-methods/{id}", 1L)).andExpect(status().isNoContent());

    verify(brewMethodService).delete(1L);
  }

  @Test
  void create_shouldReturnBadRequest_whenNameExceedsMaxLength() throws Exception {
    BrewMethodRequest request =
        new BrewMethodRequest("A".repeat(81), "Immersion and pressure-based brewing method.");

    mockMvc
        .perform(
            post("/api/brew-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(
            jsonPath("$.validationErrors.name").value("Method name must not exceed 80 characters"));
  }

  @Test
  void create_shouldReturnBadRequest_whenDescriptionExceedsMaxLength() throws Exception {
    BrewMethodRequest request = new BrewMethodRequest("AeroPress", "A".repeat(501));

    mockMvc
        .perform(
            post("/api/brew-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(
            jsonPath("$.validationErrors.description")
                .value("Method description must not exceed 500 characters"));
  }

  @Test
  void update_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
    BrewMethodRequest request = new BrewMethodRequest("", "Pour-over brewing method.");

    mockMvc
        .perform(
            put("/api/brew-methods/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.name").value("Method name is required"));
  }
}
