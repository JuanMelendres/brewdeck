package com.brewdeck.brewdeck_api.coffee;

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
class CoffeeServiceTest {

  @Mock private CoffeeRepository coffeeRepository;

  @InjectMocks private CoffeeService coffeeService;

  @Test
  void findAll_shouldReturnAllCoffees() {
    Coffee coffee =
        Coffee.builder()
            .id(1L)
            .name("Mezcla Veracruz")
            .brand("Café local")
            .origin("Veracruz")
            .notesPrimary("Cardamomo")
            .createdAt(LocalDateTime.now())
            .build();

    when(coffeeRepository.findAll()).thenReturn(List.of(coffee));

    List<CoffeeResponse> result = coffeeService.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().id()).isEqualTo(1L);
    assertThat(result.getFirst().name()).isEqualTo("Mezcla Veracruz");

    verify(coffeeRepository).findAll();
  }

  @Test
  void findById_shouldReturnCoffee_whenCoffeeExists() {
    Coffee coffee =
        Coffee.builder().id(1L).name("Mezcla Veracruz").createdAt(LocalDateTime.now()).build();

    when(coffeeRepository.findById(1L)).thenReturn(Optional.of(coffee));

    CoffeeResponse result = coffeeService.findById(1L);

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo("Mezcla Veracruz");

    verify(coffeeRepository).findById(1L);
  }

  @Test
  void findById_shouldThrowException_whenCoffeeDoesNotExist() {
    when(coffeeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> coffeeService.findById(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Coffee not found");

    verify(coffeeRepository).findById(99L);
  }

  @Test
  void create_shouldSaveCoffee() {
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
            "Café limpio y aromático.");

    Coffee savedCoffee =
        Coffee.builder()
            .id(1L)
            .name(request.name())
            .brand(request.brand())
            .origin(request.origin())
            .region(request.region())
            .farm(request.farm())
            .producer(request.producer())
            .variety(request.variety())
            .process(request.process())
            .roastLevel(request.roastLevel())
            .notesPrimary(request.notesPrimary())
            .notesSecondary(request.notesSecondary())
            .acidity(request.acidity())
            .body(request.body())
            .sweetness(request.sweetness())
            .bitterness(request.bitterness())
            .description(request.description())
            .createdAt(LocalDateTime.now())
            .build();

    when(coffeeRepository.save(any(Coffee.class))).thenReturn(savedCoffee);

    CoffeeResponse result = coffeeService.create(request);

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo("Mezcla Veracruz");
    assertThat(result.notesPrimary()).isEqualTo("Cardamomo");

    verify(coffeeRepository).save(any(Coffee.class));
  }

  @Test
  void delete_shouldDeleteCoffee_whenCoffeeExists() {
    when(coffeeRepository.existsById(1L)).thenReturn(true);

    coffeeService.delete(1L);

    verify(coffeeRepository).existsById(1L);
    verify(coffeeRepository).deleteById(1L);
  }

  @Test
  void delete_shouldThrowException_whenCoffeeDoesNotExist() {
    when(coffeeRepository.existsById(99L)).thenReturn(false);

    assertThatThrownBy(() -> coffeeService.delete(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Coffee not found");

    verify(coffeeRepository).existsById(99L);
    verify(coffeeRepository, never()).deleteById(anyLong());
  }
}
