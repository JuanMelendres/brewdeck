package com.brewdeck.brewdeck_api.method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brewdeck.brewdeck_api.common.PostgresRepositoryTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class BrewMethodRepositoryTest extends PostgresRepositoryTest {

  @Autowired private BrewMethodRepository brewMethodRepository;

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
}
