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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

    when(coffeeService.findAll()).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/coffees"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].name").value("Mezcla Veracruz"))
        .andExpect(jsonPath("$[0].notesPrimary").value("Cardamomo"));

    verify(coffeeService).findAll();
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
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("Mezcla Veracruz"));

    verify(coffeeService).create(any(CoffeeRequest.class));
  }

  @Test
  void create_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
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
        .andExpect(status().isBadRequest());
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

    mockMvc.perform(delete("/api/coffees/{id}", 1L)).andExpect(status().isOk());

    verify(coffeeService).delete(1L);
  }
}
