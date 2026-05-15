package com.brewdeck.brewdeck_api.coffee;

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
import jakarta.persistence.EntityNotFoundException;
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

@WebMvcTest(CoffeeController.class)
class CoffeeControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private CoffeeService coffeeService;

  @Test
  void findAll_shouldReturnCoffees() throws Exception {
    CoffeeResponse response =
        new CoffeeResponse(
            1L,
            "Mezcla Veracruz",
            "Café local",
            "Veracruz",
            null,
            null,
            null,
            "Blend",
            "Lavado",
            "Medio",
            "Cardamomo",
            "Canela, clavo",
            "Media",
            "Medio",
            "Media",
            "Baja",
            "Café limpio y aromático",
            LocalDateTime.now(),
            null);

    CoffeeFilter filter = new CoffeeFilter(null, null, null, null);

    PageResponse<CoffeeResponse> pageResponse =
        PageResponse.fromPage(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

    when(coffeeService.search(eq(filter), any(Pageable.class))).thenReturn(pageResponse);

    mockMvc
        .perform(get("/api/coffees"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(1L))
        .andExpect(jsonPath("$.content[0].name").value("Mezcla Veracruz"))
        .andExpect(jsonPath("$.content[0].notesPrimary").value("Cardamomo"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.first").value(true))
        .andExpect(jsonPath("$.last").value(true));

    verify(coffeeService).search(eq(filter), any(Pageable.class));
  }

  @Test
  void findAll_shouldReturnInternalServerError_whenUnexpectedExceptionOccurs() throws Exception {
    when(coffeeService.search(any(CoffeeFilter.class), any(Pageable.class)))
        .thenThrow(new RuntimeException("Unexpected"));

    mockMvc
        .perform(get("/api/coffees"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.error").value("Internal Server Error"))
        .andExpect(jsonPath("$.message").value("Unexpected error occurred"));

    verify(coffeeService).search(any(CoffeeFilter.class), any(Pageable.class));
  }

  @Test
  void findAll_shouldReturnFilteredCoffees() throws Exception {
    CoffeeResponse response =
        new CoffeeResponse(
            1L,
            "Mezcla Veracruz",
            "Café local",
            "Veracruz",
            null,
            null,
            null,
            "Blend",
            "Lavado",
            "Medio",
            "Cardamomo",
            "Canela, clavo",
            "Media",
            "Medio",
            "Media",
            "Baja",
            "Café limpio y aromático",
            LocalDateTime.now(),
            null);

    CoffeeFilter filter = new CoffeeFilter("Veracruz", "Veracruz", "Medio", "Lavado");

    PageResponse<CoffeeResponse> pageResponse =
        PageResponse.fromPage(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

    when(coffeeService.search(eq(filter), any(Pageable.class))).thenReturn(pageResponse);

    mockMvc
        .perform(
            get("/api/coffees")
                .param("name", "Veracruz")
                .param("origin", "Veracruz")
                .param("roastLevel", "Medio")
                .param("process", "Lavado")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "id,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Mezcla Veracruz"))
        .andExpect(jsonPath("$.content[0].origin").value("Veracruz"))
        .andExpect(jsonPath("$.content[0].roastLevel").value("Medio"))
        .andExpect(jsonPath("$.content[0].process").value("Lavado"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(coffeeService).search(eq(filter), any(Pageable.class));
  }

  @Test
  void findById_shouldReturnCoffee() throws Exception {
    CoffeeResponse response =
        new CoffeeResponse(
            1L,
            "Mezcla Veracruz",
            "Café local",
            "Veracruz",
            null,
            null,
            null,
            "Blend",
            "Lavado",
            "Medio",
            "Cardamomo",
            "Canela, clavo",
            "Media",
            "Medio",
            "Media",
            "Baja",
            "Café limpio y aromático",
            LocalDateTime.now(),
            null);

    when(coffeeService.findById(1L)).thenReturn(response);

    mockMvc
        .perform(get("/api/coffees/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("Mezcla Veracruz"));

    verify(coffeeService).findById(1L);
  }

  @Test
  void findById_shouldReturnNotFound_whenCoffeeDoesNotExist() throws Exception {
    when(coffeeService.findById(99L)).thenThrow(new EntityNotFoundException("Coffee not found"));

    mockMvc
        .perform(get("/api/coffees/{id}", 99L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Coffee not found"))
        .andExpect(jsonPath("$.path").value("/api/coffees/99"));

    verify(coffeeService).findById(99L);
  }

  @Test
  void create_shouldReturnCreatedCoffee() throws Exception {
    CoffeeRequest request =
        new CoffeeRequest(
            "Mezcla Veracruz",
            "Café local",
            "Veracruz",
            null,
            null,
            null,
            "Blend",
            "Lavado",
            "Medio",
            "Cardamomo",
            "Canela, clavo",
            "Media",
            "Medio",
            "Media",
            "Baja",
            "Café limpio y aromático");

    CoffeeResponse response =
        new CoffeeResponse(
            1L,
            request.name(),
            request.brand(),
            request.origin(),
            request.region(),
            request.farm(),
            request.producer(),
            request.variety(),
            request.process(),
            request.roastLevel(),
            request.notesPrimary(),
            request.notesSecondary(),
            request.acidity(),
            request.body(),
            request.sweetness(),
            request.bitterness(),
            request.description(),
            LocalDateTime.now(),
            null);

    when(coffeeService.create(any(CoffeeRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/coffees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("Mezcla Veracruz"));

    verify(coffeeService).create(any(CoffeeRequest.class));
  }

  @Test
  void create_shouldReturnValidationErrorBody_whenNameIsBlank() throws Exception {
    CoffeeRequest request =
        new CoffeeRequest(
            "",
            "Café local",
            "Veracruz",
            null,
            null,
            null,
            "Blend",
            "Lavado",
            "Medio",
            "Cardamomo",
            "Canela, clavo",
            "Media",
            "Medio",
            "Media",
            "Baja",
            "Café limpio y aromático");

    mockMvc
        .perform(
            post("/api/coffees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.name").value("Coffee name is required"));
  }

  @Test
  void update_shouldReturnUpdatedCoffee() throws Exception {
    CoffeeRequest request =
        new CoffeeRequest(
            "Mezcla Veracruz Updated",
            "Café local",
            "Veracruz",
            null,
            null,
            null,
            "Blend",
            "Lavado",
            "Medio",
            "Cardamomo",
            "Canela, clavo",
            "Media",
            "Medio",
            "Media",
            "Baja",
            "Updated description");

    CoffeeResponse response =
        new CoffeeResponse(
            1L,
            request.name(),
            request.brand(),
            request.origin(),
            request.region(),
            request.farm(),
            request.producer(),
            request.variety(),
            request.process(),
            request.roastLevel(),
            request.notesPrimary(),
            request.notesSecondary(),
            request.acidity(),
            request.body(),
            request.sweetness(),
            request.bitterness(),
            request.description(),
            LocalDateTime.now(),
            LocalDateTime.now());

    when(coffeeService.update(eq(1L), any(CoffeeRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            put("/api/coffees/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("Mezcla Veracruz Updated"));

    verify(coffeeService).update(eq(1L), any(CoffeeRequest.class));
  }

  @Test
  void delete_shouldDeleteCoffee() throws Exception {
    doNothing().when(coffeeService).delete(1L);

    mockMvc.perform(delete("/api/coffees/{id}", 1L)).andExpect(status().isNoContent());

    verify(coffeeService).delete(1L);
  }

  @Test
  void create_shouldReturnBadRequest_whenNameExceedsMaxLength() throws Exception {
    CoffeeRequest request =
        new CoffeeRequest(
            "A".repeat(121),
            "Café local",
            "Veracruz",
            null,
            null,
            null,
            "Blend",
            "Lavado",
            "Medio",
            "Cardamomo",
            "Canela, clavo",
            "Media",
            "Medio",
            "Media",
            "Baja",
            "Café limpio y aromático");

    mockMvc
        .perform(
            post("/api/coffees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(
            jsonPath("$.validationErrors.name")
                .value("Coffee name must not exceed 120 characters"));
  }

  @Test
  void create_shouldReturnBadRequest_whenDescriptionExceedsMaxLength() throws Exception {
    CoffeeRequest request =
        new CoffeeRequest(
            "Mezcla Veracruz",
            "Café local",
            "Veracruz",
            null,
            null,
            null,
            "Blend",
            "Lavado",
            "Medio",
            "Cardamomo",
            "Canela, clavo",
            "Media",
            "Medio",
            "Media",
            "Baja",
            "A".repeat(1001));

    mockMvc
        .perform(
            post("/api/coffees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(
            jsonPath("$.validationErrors.description")
                .value("Description must not exceed 1000 characters"));
  }

  @Test
  void update_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
    CoffeeRequest request =
        new CoffeeRequest(
            "",
            "Café local",
            "Veracruz",
            null,
            null,
            null,
            "Blend",
            "Lavado",
            "Medio",
            "Cardamomo",
            "Canela, clavo",
            "Media",
            "Medio",
            "Media",
            "Baja",
            "Updated description");

    mockMvc
        .perform(
            put("/api/coffees/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.name").value("Coffee name is required"));
  }
}
