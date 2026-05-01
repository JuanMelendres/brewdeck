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
    BrewMethod method =
        BrewMethod.builder()
            .name("AeroPress")
            .description("Immersion and pressure-based brewing method.")
            .build();

    BrewMethod savedMethod = brewMethodRepository.save(method);

    assertThat(savedMethod.getId()).isNotNull();
    assertThat(savedMethod.getName()).isEqualTo("AeroPress");
    assertThat(savedMethod.getCreatedAt()).isNotNull();
  }

  @Test
  void findById_shouldReturnBrewMethod_whenMethodExists() {
    BrewMethod method =
        BrewMethod.builder().name("V60").description("Pour-over brewing method.").build();

    BrewMethod savedMethod = brewMethodRepository.save(method);

    Optional<BrewMethod> result = brewMethodRepository.findById(savedMethod.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("V60");
  }

  @Test
  void findAll_shouldReturnAllBrewMethods() {
    BrewMethod methodOne =
        BrewMethod.builder()
            .name("Espresso")
            .description("Pressure-based extraction method.")
            .build();

    BrewMethod methodTwo =
        BrewMethod.builder().name("French Press").description("Immersion brewing method.").build();

    brewMethodRepository.save(methodOne);
    brewMethodRepository.save(methodTwo);

    assertThat(brewMethodRepository.findAll()).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void save_shouldThrowException_whenNameIsDuplicated() {
    BrewMethod methodOne =
        BrewMethod.builder().name("Chemex").description("First Chemex method.").build();

    BrewMethod methodTwo =
        BrewMethod.builder().name("Chemex").description("Duplicated Chemex method.").build();

    brewMethodRepository.saveAndFlush(methodOne);

    assertThatThrownBy(() -> brewMethodRepository.saveAndFlush(methodTwo))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void delete_shouldRemoveBrewMethod() {
    BrewMethod method =
        BrewMethod.builder().name("Method to Delete").description("Temporary method.").build();

    BrewMethod savedMethod = brewMethodRepository.save(method);

    brewMethodRepository.deleteById(savedMethod.getId());

    Optional<BrewMethod> result = brewMethodRepository.findById(savedMethod.getId());

    assertThat(result).isEmpty();
  }
}
