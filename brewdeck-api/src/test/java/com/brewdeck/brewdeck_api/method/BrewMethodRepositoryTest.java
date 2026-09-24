package com.brewdeck.brewdeck_api.method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.common.PostgresRepositoryTest;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class BrewMethodRepositoryTest extends PostgresRepositoryTest {

  @Autowired private BrewMethodRepository brewMethodRepository;
  @Autowired private UserRepository userRepository;

  @Test
  void save_shouldPersistBrewMethod() {
    String methodName = "Test Method " + System.nanoTime();

    BrewMethod method =
        BrewMethod.builder()
            .name(methodName)
            .description("Brew method created for repository tests.")
            .build();

    BrewMethod savedMethod = brewMethodRepository.save(method);

    assertThat(savedMethod.getId()).isNotNull();
    assertThat(savedMethod.getName()).isEqualTo(methodName);
    assertThat(savedMethod.getCreatedAt()).isNotNull();
  }

  @Test
  void findById_shouldReturnBrewMethod_whenMethodExists() {
    String methodName = "Test Method " + System.nanoTime();

    BrewMethod method =
        BrewMethod.builder()
            .name(methodName)
            .description("Brew method created for repository tests.")
            .build();

    BrewMethod savedMethod = brewMethodRepository.save(method);

    Optional<BrewMethod> result = brewMethodRepository.findById(savedMethod.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo(methodName);
  }

  @Test
  void findAll_shouldReturnAllBrewMethods() {
    BrewMethod methodOne =
        BrewMethod.builder()
            .name("Test Method " + System.nanoTime())
            .description("Brew method created for repository tests.")
            .build();

    BrewMethod methodTwo =
        BrewMethod.builder()
            .name("Test Method " + System.nanoTime())
            .description("Brew method created for repository tests.")
            .build();

    brewMethodRepository.save(methodOne);
    brewMethodRepository.save(methodTwo);

    assertThat(brewMethodRepository.findAll()).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void save_shouldThrowException_whenNameIsDuplicated() {

    BrewMethod methodTwo =
        BrewMethod.builder().name("Chemex").description("Duplicated Chemex method.").build();

    assertThatThrownBy(() -> brewMethodRepository.saveAndFlush(methodTwo))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void delete_shouldRemoveBrewMethod() {
    BrewMethod method =
        BrewMethod.builder()
            .name("Test Method " + System.nanoTime())
            .description("Brew method created for repository tests.")
            .build();

    BrewMethod savedMethod = brewMethodRepository.save(method);

    brewMethodRepository.deleteById(savedMethod.getId());

    Optional<BrewMethod> result = brewMethodRepository.findById(savedMethod.getId());

    assertThat(result).isEmpty();
  }

  private User user() {
    return userRepository.save(
        User.builder()
            .email("method-repo-" + System.nanoTime() + "@example.com")
            .passwordHash("hash")
            .createdAt(LocalDateTime.now())
            .build());
  }

  private BrewMethod method(String name, User owner) {
    return brewMethodRepository.saveAndFlush(BrewMethod.builder().name(name).owner(owner).build());
  }

  @Test
  void findVisibleTo_returnsSharedAndOwnButNotForeignPrivateMethods() {
    User me = user();
    User other = user();
    BrewMethod shared = method("Shared " + System.nanoTime(), null);
    BrewMethod mine = method("Mine " + System.nanoTime(), me);
    BrewMethod foreign = method("Foreign " + System.nanoTime(), other);

    var visible = brewMethodRepository.findVisibleTo(me.getId(), PageRequest.of(0, 1000));

    assertThat(visible.getContent())
        .extracting(BrewMethod::getId)
        .contains(shared.getId(), mine.getId())
        .doesNotContain(foreign.getId());
    assertThat(brewMethodRepository.findVisibleById(foreign.getId(), me.getId())).isEmpty();
    assertThat(brewMethodRepository.findVisibleById(mine.getId(), me.getId())).isPresent();
    assertThat(brewMethodRepository.countVisibleTo(me.getId()))
        .isEqualTo(visible.getTotalElements());
  }

  @Test
  void findByIdAndOwnerIsNull_ignoresPrivateMethods() {
    BrewMethod mine = method("Private " + System.nanoTime(), user());

    assertThat(brewMethodRepository.findByIdAndOwnerIsNull(mine.getId())).isEmpty();
  }

  @Test
  void privateName_mayRepeatAcrossOwnersAndTheSharedCatalog() {
    String name = "Pour Over " + System.nanoTime();
    method(name, null);
    method(name, user());

    BrewMethod secondOwner = method(name, user());

    assertThat(secondOwner.getId()).isNotNull();
  }

  @Test
  void privateName_mustBeUniquePerOwner() {
    User me = user();
    String name = "Duplicate Mine " + System.nanoTime();
    method(name, me);

    BrewMethod duplicate = BrewMethod.builder().name(name).owner(me).build();

    assertThatThrownBy(() -> brewMethodRepository.saveAndFlush(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
