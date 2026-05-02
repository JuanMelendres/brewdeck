package com.brewdeck.brewdeck_api.method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrewMethodServiceTest {

  @Mock private BrewMethodRepository brewMethodRepository;

  @InjectMocks private BrewMethodService brewMethodService;

  @Test
  void findAll_shouldReturnAllBrewMethods() {
    BrewMethod method =
        BrewMethod.builder()
            .id(1L)
            .name("AeroPress")
            .description("Immersion and pressure-based brewing method.")
            .createdAt(LocalDateTime.now())
            .build();

    when(brewMethodRepository.findAll()).thenReturn(List.of(method));

    List<BrewMethodResponse> result = brewMethodService.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().id()).isEqualTo(1L);
    assertThat(result.getFirst().name()).isEqualTo("AeroPress");

    verify(brewMethodRepository).findAll();
  }

  @Test
  void findById_shouldReturnBrewMethod_whenExists() {
    BrewMethod method =
        BrewMethod.builder().id(1L).name("V60").createdAt(LocalDateTime.now()).build();

    when(brewMethodRepository.findById(1L)).thenReturn(Optional.of(method));

    BrewMethodResponse result = brewMethodService.findById(1L);

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo("V60");

    verify(brewMethodRepository).findById(1L);
  }

  @Test
  void findById_shouldThrowException_whenNotFound() {
    when(brewMethodRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> brewMethodService.findById(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Brew method not found");

    verify(brewMethodRepository).findById(99L);
  }

  @Test
  void create_shouldSaveBrewMethod() {
    BrewMethodRequest request =
        new BrewMethodRequest("Espresso", "Pressure-based extraction method.");

    BrewMethod savedMethod =
        BrewMethod.builder()
            .id(1L)
            .name(request.name())
            .description(request.description())
            .createdAt(LocalDateTime.now())
            .build();

    when(brewMethodRepository.save(any(BrewMethod.class))).thenReturn(savedMethod);

    BrewMethodResponse result = brewMethodService.create(request);

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo("Espresso");

    verify(brewMethodRepository).save(any(BrewMethod.class));
  }

  @Test
  void update_shouldUpdateBrewMethod_whenExists() {
    BrewMethod existingMethod =
        BrewMethod.builder()
            .id(1L)
            .name("Old Method")
            .description("Old description")
            .createdAt(LocalDateTime.now())
            .build();

    BrewMethodRequest request = new BrewMethodRequest("AeroPress", "Updated description");

    when(brewMethodRepository.findById(1L)).thenReturn(Optional.of(existingMethod));
    when(brewMethodRepository.save(existingMethod)).thenReturn(existingMethod);

    BrewMethodResponse result = brewMethodService.update(1L, request);

    assertThat(result.name()).isEqualTo("AeroPress");
    assertThat(result.description()).isEqualTo("Updated description");

    verify(brewMethodRepository).findById(1L);
    verify(brewMethodRepository).save(existingMethod);
  }

  @Test
  void delete_shouldDeleteBrewMethod_whenExists() {
    when(brewMethodRepository.existsById(1L)).thenReturn(true);

    brewMethodService.delete(1L);

    verify(brewMethodRepository).existsById(1L);
    verify(brewMethodRepository).deleteById(1L);
  }
}
