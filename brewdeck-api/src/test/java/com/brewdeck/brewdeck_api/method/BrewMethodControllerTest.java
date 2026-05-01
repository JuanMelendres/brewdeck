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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BrewMethodController.class)
class BrewMethodControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private BrewMethodService brewMethodService;

  @Test
  void findAll_shouldReturnBrewMethods() throws Exception {
    BrewMethodResponse response =
        new BrewMethodResponse(
            1L, "AeroPress", "Immersion and pressure-based brewing method.", LocalDateTime.now());

    when(brewMethodService.findAll()).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/brew-methods"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].name").value("AeroPress"));

    verify(brewMethodService).findAll();
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
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("AeroPress"));

    verify(brewMethodService).create(any(BrewMethodRequest.class));
  }

  @Test
  void create_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
    BrewMethodRequest request =
        new BrewMethodRequest("", "Immersion and pressure-based brewing method.");

    mockMvc
        .perform(
            post("/api/brew-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
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

    mockMvc.perform(delete("/api/brew-methods/{id}", 1L)).andExpect(status().isOk());

    verify(brewMethodService).delete(1L);
  }
}
