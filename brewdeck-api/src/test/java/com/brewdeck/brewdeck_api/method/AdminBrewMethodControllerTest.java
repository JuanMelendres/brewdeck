package com.brewdeck.brewdeck_api.method;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AdminBrewMethodController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
              com.brewdeck.brewdeck_api.auth.JwtAuthenticationFilter.class,
              com.brewdeck.brewdeck_api.common.config.SecurityConfig.class,
              com.brewdeck.brewdeck_api.common.config.RestAuthenticationEntryPoint.class
            }))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMIN")
class AdminBrewMethodControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private BrewMethodService brewMethodService;

  @Test
  void create_shouldReturnCreatedSharedMethod() throws Exception {
    BrewMethodRequest request = new BrewMethodRequest("Siphon", "Vacuum brewer.");
    when(brewMethodService.createShared(any(BrewMethodRequest.class)))
        .thenReturn(
            new BrewMethodResponse(
                5L, request.name(), request.description(), true, LocalDateTime.now()));

    mockMvc
        .perform(
            post("/api/admin/brew-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/brew-methods/5"))
        .andExpect(jsonPath("$.shared").value(true));

    verify(brewMethodService).createShared(any(BrewMethodRequest.class));
  }

  @Test
  void create_shouldReturnValidationError_whenNameIsBlank() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/brew-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BrewMethodRequest("", null))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.name").value("Method name is required"));
  }

  @Test
  void update_shouldReturnUpdatedSharedMethod() throws Exception {
    BrewMethodRequest request = new BrewMethodRequest("Hario V60", null);
    when(brewMethodService.updateShared(eq(1L), any(BrewMethodRequest.class)))
        .thenReturn(new BrewMethodResponse(1L, "Hario V60", null, true, LocalDateTime.now()));

    mockMvc
        .perform(
            put("/api/admin/brew-methods/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Hario V60"));
  }

  @Test
  void delete_shouldReturnNoContent() throws Exception {
    mockMvc.perform(delete("/api/admin/brew-methods/{id}", 1L)).andExpect(status().isNoContent());

    verify(brewMethodService).deleteShared(1L);
  }

  @Test
  void delete_shouldReturnNotFound_whenNotInSharedCatalog() throws Exception {
    doThrow(new EntityNotFoundException("Brew method not found"))
        .when(brewMethodService)
        .deleteShared(2L);

    mockMvc
        .perform(delete("/api/admin/brew-methods/{id}", 2L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Brew method not found"));
  }
}
