package com.brewdeck.brewdeck_api.coffee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.common.pagination.PageResponse;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CoffeeServiceTest {

  @Mock private CoffeeRepository coffeeRepository;

  @Mock private RecipeRepository recipeRepository;

  @Mock private CurrentUserProvider currentUserProvider;

  @InjectMocks private CoffeeService coffeeService;

  @Test
  void getMostUsed_shouldMapRowsPreservingOrder() {
    when(recipeRepository.findMostUsedCoffees(any(Pageable.class)))
        .thenReturn(List.of(mostUsed(2L, "Popular", 7L), mostUsed(1L, "Rare", 1L)));

    List<MostUsedCoffeeResponse> result = coffeeService.getMostUsed(5);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).coffeeId()).isEqualTo(2L);
    assertThat(result.get(0).coffeeName()).isEqualTo("Popular");
    assertThat(result.get(0).recipeCount()).isEqualTo(7L);
    assertThat(result.get(1).coffeeId()).isEqualTo(1L);
  }

  @Test
  void getMostUsed_shouldClampLimitToRange() {
    when(recipeRepository.findMostUsedCoffees(any(Pageable.class))).thenReturn(List.of());
    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

    coffeeService.getMostUsed(0);
    coffeeService.getMostUsed(999);

    verify(recipeRepository, times(2)).findMostUsedCoffees(captor.capture());
    assertThat(captor.getAllValues().get(0)).isEqualTo(PageRequest.of(0, 1));
    assertThat(captor.getAllValues().get(1).getPageSize()).isEqualTo(20);
  }

  private MostUsedCoffee mostUsed(Long id, String name, long count) {
    return new MostUsedCoffee() {
      @Override
      public Long getCoffeeId() {
        return id;
      }

      @Override
      public String getCoffeeName() {
        return name;
      }

      @Override
      public long getRecipeCount() {
        return count;
      }
    };
  }

  @Test
  void search_shouldReturnPagedCoffees_whenFilterIsEmpty() {
    Coffee coffee =
        Coffee.builder()
            .id(1L)
            .name("Mezcla Veracruz")
            .brand("Café local")
            .origin("Veracruz")
            .notesPrimary("Cardamomo")
            .createdAt(LocalDateTime.now())
            .build();

    Pageable pageable = PageRequest.of(0, 10);

    when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
    when(coffeeRepository.findAll(anyCoffeeSpecification(), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(coffee), pageable, 1));

    PageResponse<CoffeeResponse> result =
        coffeeService.search(new CoffeeFilter(null, null, null, null), pageable);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().getFirst().id()).isEqualTo(1L);
    assertThat(result.content().getFirst().name()).isEqualTo("Mezcla Veracruz");
    assertThat(result.page()).isZero();
    assertThat(result.size()).isEqualTo(10);
    assertThat(result.totalElements()).isEqualTo(1);
    assertThat(result.totalPages()).isEqualTo(1);
    assertThat(result.first()).isTrue();
    assertThat(result.last()).isTrue();

    verify(coffeeRepository).findAll(anyCoffeeSpecification(), eq(pageable));
  }

  @SuppressWarnings("unchecked")
  private Specification<Coffee> anyCoffeeSpecification() {
    return any(Specification.class);
  }

  @Test
  void search_shouldReturnPagedFilteredCoffees() {
    Coffee coffee =
        Coffee.builder()
            .id(1L)
            .name("Mezcla Veracruz")
            .brand("Café local")
            .origin("Veracruz")
            .process("Lavado")
            .roastLevel("Medio")
            .notesPrimary("Cardamomo")
            .createdAt(LocalDateTime.now())
            .build();

    CoffeeFilter filter = new CoffeeFilter("Veracruz", "Veracruz", "Medio", "Lavado");
    Pageable pageable = PageRequest.of(0, 5);

    when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
    when(coffeeRepository.findAll(anyCoffeeSpecification(), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(coffee), pageable, 1));

    PageResponse<CoffeeResponse> result = coffeeService.search(filter, pageable);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().getFirst().id()).isEqualTo(1L);
    assertThat(result.content().getFirst().name()).isEqualTo("Mezcla Veracruz");
    assertThat(result.content().getFirst().origin()).isEqualTo("Veracruz");
    assertThat(result.content().getFirst().process()).isEqualTo("Lavado");
    assertThat(result.content().getFirst().roastLevel()).isEqualTo("Medio");
    assertThat(result.page()).isZero();
    assertThat(result.size()).isEqualTo(5);
    assertThat(result.totalElements()).isEqualTo(1);

    verify(coffeeRepository).findAll(anyCoffeeSpecification(), eq(pageable));
  }

  @Test
  void findById_shouldReturnCoffee_whenCoffeeExists() {
    Coffee coffee =
        Coffee.builder().id(1L).name("Mezcla Veracruz").createdAt(LocalDateTime.now()).build();

    when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
    when(coffeeRepository.findByIdAndOwnerId(1L, 42L)).thenReturn(Optional.of(coffee));

    CoffeeResponse result = coffeeService.findById(1L);

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo("Mezcla Veracruz");

    verify(coffeeRepository).findByIdAndOwnerId(1L, 42L);
  }

  @Test
  void findById_shouldThrowException_whenCoffeeDoesNotExist() {
    when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
    when(coffeeRepository.findByIdAndOwnerId(99L, 42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> coffeeService.findById(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Coffee not found");

    verify(coffeeRepository).findByIdAndOwnerId(99L, 42L);
  }

  @Test
  void findById_shouldThrow_whenCoffeeOwnedByAnotherUser() {
    when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
    when(coffeeRepository.findByIdAndOwnerId(99L, 42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> coffeeService.findById(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Coffee not found");
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
            3,
            3,
            4,
            2,
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
            .acidityScore(request.acidityScore())
            .bodyScore(request.bodyScore())
            .sweetnessScore(request.sweetnessScore())
            .bitternessScore(request.bitternessScore())
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
  void create_shouldStampOwnerFromCurrentUser() {
    User owner = User.builder().id(42L).email("owner@brewdeck.test").build();
    when(currentUserProvider.require()).thenReturn(owner);
    when(coffeeRepository.save(any(Coffee.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CoffeeRequest request =
        new CoffeeRequest(
            "Owned Coffee",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    coffeeService.create(request);

    ArgumentCaptor<Coffee> captor = ArgumentCaptor.forClass(Coffee.class);
    verify(coffeeRepository).save(captor.capture());
    assertThat(captor.getValue().getOwner()).isSameAs(owner);
  }

  @Test
  void delete_shouldDeleteCoffee_whenCoffeeExists() {
    when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
    when(coffeeRepository.existsByIdAndOwnerId(1L, 42L)).thenReturn(true);

    coffeeService.delete(1L);

    verify(coffeeRepository).existsByIdAndOwnerId(1L, 42L);
    verify(coffeeRepository).deleteById(1L);
  }

  @Test
  void delete_shouldThrowException_whenCoffeeDoesNotExist() {
    when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
    when(coffeeRepository.existsByIdAndOwnerId(99L, 42L)).thenReturn(false);

    assertThatThrownBy(() -> coffeeService.delete(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Coffee not found");

    verify(coffeeRepository).existsByIdAndOwnerId(99L, 42L);
    verify(coffeeRepository, never()).deleteById(anyLong());
  }

  @Test
  void update_shouldUpdateCoffee_whenCoffeeExists() {
    Coffee existingCoffee =
        Coffee.builder()
            .id(1L)
            .name("Old Coffee")
            .brand("Old Brand")
            .origin("Old Origin")
            .createdAt(LocalDateTime.now())
            .build();

    CoffeeRequest request =
        new CoffeeRequest(
            "Mezcla Veracruz Updated",
            "Café local",
            "Veracruz",
            "Coatepec",
            "Test Farm",
            "Test Producer",
            "Blend",
            "Lavado",
            "Medio",
            "Cardamomo",
            "Canela, clavo",
            3,
            3,
            4,
            2,
            "Updated description");

    when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
    when(coffeeRepository.findByIdAndOwnerId(1L, 42L)).thenReturn(Optional.of(existingCoffee));
    when(coffeeRepository.save(existingCoffee)).thenReturn(existingCoffee);

    CoffeeResponse result = coffeeService.update(1L, request);

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo("Mezcla Veracruz Updated");
    assertThat(result.brand()).isEqualTo("Café local");
    assertThat(result.origin()).isEqualTo("Veracruz");
    assertThat(result.region()).isEqualTo("Coatepec");
    assertThat(result.farm()).isEqualTo("Test Farm");
    assertThat(result.producer()).isEqualTo("Test Producer");
    assertThat(result.variety()).isEqualTo("Blend");
    assertThat(result.process()).isEqualTo("Lavado");
    assertThat(result.roastLevel()).isEqualTo("Medio");
    assertThat(result.notesPrimary()).isEqualTo("Cardamomo");
    assertThat(result.notesSecondary()).isEqualTo("Canela, clavo");
    assertThat(result.acidityScore()).isEqualTo(3);
    assertThat(result.bodyScore()).isEqualTo(3);
    assertThat(result.sweetnessScore()).isEqualTo(4);
    assertThat(result.bitternessScore()).isEqualTo(2);
    assertThat(result.description()).isEqualTo("Updated description");

    verify(coffeeRepository).findByIdAndOwnerId(1L, 42L);
    verify(coffeeRepository).save(existingCoffee);
  }

  @Test
  void update_shouldThrowException_whenCoffeeDoesNotExist() {
    CoffeeRequest request =
        new CoffeeRequest(
            "Mezcla Veracruz Updated",
            "Café local",
            "Veracruz",
            "Coatepec",
            null,
            null,
            "Blend",
            "Lavado",
            "Medio",
            "Cardamomo",
            "Canela, clavo",
            3,
            3,
            4,
            2,
            "Updated description");

    when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
    when(coffeeRepository.findByIdAndOwnerId(99L, 42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> coffeeService.update(99L, request))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Coffee not found");

    verify(coffeeRepository).findByIdAndOwnerId(99L, 42L);
    verify(coffeeRepository, never()).save(any());
  }
}
