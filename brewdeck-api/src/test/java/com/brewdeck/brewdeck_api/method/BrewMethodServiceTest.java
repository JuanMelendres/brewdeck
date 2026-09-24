package com.brewdeck.brewdeck_api.method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.common.pagination.PageResponse;
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
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class BrewMethodServiceTest {

  private static final Long OWNER_ID = 42L;

  @Mock private BrewMethodRepository brewMethodRepository;

  @Mock private CurrentUserProvider currentUserProvider;

  @InjectMocks private BrewMethodService brewMethodService;

  private final User owner = User.builder().id(OWNER_ID).build();

  private BrewMethod sharedMethod(Long id, String name) {
    return BrewMethod.builder().id(id).name(name).createdAt(LocalDateTime.now()).build();
  }

  private BrewMethod privateMethod(Long id, String name) {
    return BrewMethod.builder()
        .id(id)
        .name(name)
        .owner(owner)
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  void findAll_shouldReturnSharedAndOwnMethods() {
    Pageable pageable = PageRequest.of(0, 10);
    when(currentUserProvider.require()).thenReturn(owner);
    when(brewMethodRepository.findVisibleTo(OWNER_ID, pageable))
        .thenReturn(
            new PageImpl<>(
                List.of(sharedMethod(1L, "AeroPress"), privateMethod(2L, "My V60")), pageable, 2));

    PageResponse<BrewMethodResponse> result = brewMethodService.findAll(pageable);

    assertThat(result.content()).hasSize(2);
    assertThat(result.content().get(0).name()).isEqualTo("AeroPress");
    assertThat(result.content().get(0).shared()).isTrue();
    assertThat(result.content().get(1).name()).isEqualTo("My V60");
    assertThat(result.content().get(1).shared()).isFalse();
    assertThat(result.page()).isZero();
    assertThat(result.size()).isEqualTo(10);
    assertThat(result.totalElements()).isEqualTo(2);
    verify(brewMethodRepository).findVisibleTo(OWNER_ID, pageable);
  }

  @Test
  void getUsage_shouldMapUsageRowsPreservingOrder() {
    when(currentUserProvider.require()).thenReturn(owner);
    when(brewMethodRepository.findUsage(OWNER_ID))
        .thenReturn(List.of(usage(1L, "AeroPress", 5L), usage(2L, "V60", 0L)));

    List<MethodUsageResponse> result = brewMethodService.getUsage();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).methodId()).isEqualTo(1L);
    assertThat(result.get(0).methodName()).isEqualTo("AeroPress");
    assertThat(result.get(0).recipeCount()).isEqualTo(5L);
    assertThat(result.get(1).methodName()).isEqualTo("V60");
    assertThat(result.get(1).recipeCount()).isZero();

    verify(brewMethodRepository).findUsage(OWNER_ID);
  }

  private MethodUsage usage(Long id, String name, long count) {
    return new MethodUsage() {
      @Override
      public Long getMethodId() {
        return id;
      }

      @Override
      public String getMethodName() {
        return name;
      }

      @Override
      public long getRecipeCount() {
        return count;
      }
    };
  }

  @Test
  void findById_shouldReturnVisibleMethod() {
    when(currentUserProvider.require()).thenReturn(owner);
    when(brewMethodRepository.findVisibleById(1L, OWNER_ID))
        .thenReturn(Optional.of(sharedMethod(1L, "V60")));

    BrewMethodResponse result = brewMethodService.findById(1L);

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo("V60");
    assertThat(result.shared()).isTrue();
  }

  @Test
  void findById_shouldThrowNotFound_whenMissingOrOwnedByAnotherUser() {
    when(currentUserProvider.require()).thenReturn(owner);
    when(brewMethodRepository.findVisibleById(99L, OWNER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> brewMethodService.findById(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Brew method not found");
  }

  @Test
  void create_shouldSavePrivateMethodOwnedByCurrentUser() {
    BrewMethodRequest request = new BrewMethodRequest("My Espresso", "Home machine.");
    when(currentUserProvider.require()).thenReturn(owner);
    when(brewMethodRepository.save(any(BrewMethod.class)))
        .thenAnswer(
            invocation -> {
              BrewMethod method = invocation.getArgument(0);
              method.setId(1L);
              return method;
            });

    BrewMethodResponse result = brewMethodService.create(request);

    ArgumentCaptor<BrewMethod> captor = ArgumentCaptor.forClass(BrewMethod.class);
    verify(brewMethodRepository).save(captor.capture());
    assertThat(captor.getValue().getOwner()).isSameAs(owner);
    assertThat(result.name()).isEqualTo("My Espresso");
    assertThat(result.shared()).isFalse();
  }

  @Test
  void update_shouldUpdateOwnPrivateMethod() {
    BrewMethod existing = privateMethod(1L, "Old Method");
    BrewMethodRequest request = new BrewMethodRequest("AeroPress Go", "Updated description");
    when(currentUserProvider.require()).thenReturn(owner);
    when(brewMethodRepository.findVisibleById(1L, OWNER_ID)).thenReturn(Optional.of(existing));
    when(brewMethodRepository.save(existing)).thenReturn(existing);

    BrewMethodResponse result = brewMethodService.update(1L, request);

    assertThat(result.name()).isEqualTo("AeroPress Go");
    assertThat(result.description()).isEqualTo("Updated description");
  }

  @Test
  void update_shouldBeForbidden_forSharedMethod() {
    BrewMethod shared = sharedMethod(1L, "V60");
    when(currentUserProvider.require()).thenReturn(owner);
    when(brewMethodRepository.findVisibleById(1L, OWNER_ID)).thenReturn(Optional.of(shared));
    BrewMethodRequest request = new BrewMethodRequest("Hijacked", null);

    assertThatThrownBy(() -> brewMethodService.update(1L, request))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(shared.getName()).isEqualTo("V60");
    verify(brewMethodRepository, never()).save(any());
  }

  @Test
  void update_shouldThrowNotFound_whenNotVisible() {
    when(currentUserProvider.require()).thenReturn(owner);
    when(brewMethodRepository.findVisibleById(99L, OWNER_ID)).thenReturn(Optional.empty());
    BrewMethodRequest request = new BrewMethodRequest("Name", null);

    assertThatThrownBy(() -> brewMethodService.update(99L, request))
        .isInstanceOf(EntityNotFoundException.class);
    verify(brewMethodRepository, never()).save(any());
  }

  @Test
  void delete_shouldDeleteOwnPrivateMethod() {
    BrewMethod existing = privateMethod(1L, "Mine");
    when(currentUserProvider.require()).thenReturn(owner);
    when(brewMethodRepository.findVisibleById(1L, OWNER_ID)).thenReturn(Optional.of(existing));

    brewMethodService.delete(1L);

    verify(brewMethodRepository).delete(existing);
  }

  @Test
  void delete_shouldBeForbidden_forSharedMethod() {
    when(currentUserProvider.require()).thenReturn(owner);
    when(brewMethodRepository.findVisibleById(1L, OWNER_ID))
        .thenReturn(Optional.of(sharedMethod(1L, "V60")));

    assertThatThrownBy(() -> brewMethodService.delete(1L))
        .isInstanceOf(AccessDeniedException.class);
    verify(brewMethodRepository, never()).delete(any());
  }

  @Test
  void delete_shouldThrowNotFound_whenNotVisible() {
    when(currentUserProvider.require()).thenReturn(owner);
    when(brewMethodRepository.findVisibleById(99L, OWNER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> brewMethodService.delete(99L))
        .isInstanceOf(EntityNotFoundException.class);
    verify(brewMethodRepository, never()).delete(any());
  }

  @Test
  void createShared_shouldSaveMethodWithoutOwner() {
    when(brewMethodRepository.save(any(BrewMethod.class)))
        .thenAnswer(
            invocation -> {
              BrewMethod method = invocation.getArgument(0);
              method.setId(7L);
              return method;
            });

    BrewMethodResponse result =
        brewMethodService.createShared(new BrewMethodRequest("Siphon", "Vacuum brewer."));

    assertThat(result.shared()).isTrue();
    assertThat(result.name()).isEqualTo("Siphon");
    verifyNoInteractions(currentUserProvider);
  }

  @Test
  void updateShared_shouldUpdateCatalogMethod() {
    BrewMethod shared = sharedMethod(1L, "V60");
    when(brewMethodRepository.findByIdAndOwnerIsNull(1L)).thenReturn(Optional.of(shared));
    when(brewMethodRepository.save(shared)).thenReturn(shared);

    BrewMethodResponse result =
        brewMethodService.updateShared(1L, new BrewMethodRequest("Hario V60", null));

    assertThat(result.name()).isEqualTo("Hario V60");
  }

  @Test
  void updateShared_shouldThrowNotFound_forPrivateMethod() {
    when(brewMethodRepository.findByIdAndOwnerIsNull(2L)).thenReturn(Optional.empty());
    BrewMethodRequest request = new BrewMethodRequest("Name", null);

    assertThatThrownBy(() -> brewMethodService.updateShared(2L, request))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void deleteShared_shouldDeleteCatalogMethod() {
    BrewMethod shared = sharedMethod(1L, "V60");
    when(brewMethodRepository.findByIdAndOwnerIsNull(1L)).thenReturn(Optional.of(shared));

    brewMethodService.deleteShared(1L);

    verify(brewMethodRepository).delete(shared);
  }

  @Test
  void deleteShared_shouldThrowNotFound_forPrivateMethod() {
    when(brewMethodRepository.findByIdAndOwnerIsNull(2L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> brewMethodService.deleteShared(2L))
        .isInstanceOf(EntityNotFoundException.class);
    verify(brewMethodRepository, never()).delete(any());
  }
}
