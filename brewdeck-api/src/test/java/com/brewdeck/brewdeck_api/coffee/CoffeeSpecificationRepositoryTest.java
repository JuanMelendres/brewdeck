package com.brewdeck.brewdeck_api.coffee;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.common.PostgresRepositoryTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class CoffeeSpecificationRepositoryTest extends PostgresRepositoryTest {

  @Autowired private CoffeeRepository coffeeRepository;

  @Autowired private TestEntityManager entityManager;

  @Test
  void search_shouldFilterByNameOriginRoastLevelAndProcess() {
    Coffee veracruz =
        Coffee.builder()
            .name("Mezcla Veracruz")
            .brand("Café local")
            .origin("Veracruz")
            .roastLevel("Medio")
            .process("Lavado")
            .notesPrimary("Cardamomo")
            .build();

    Coffee chiapas =
        Coffee.builder()
            .name("Chiapas Blend")
            .brand("Café local")
            .origin("Chiapas")
            .roastLevel("Oscuro")
            .process("Natural")
            .notesPrimary("Chocolate")
            .build();

    coffeeRepository.saveAll(List.of(veracruz, chiapas));

    Specification<Coffee> specification =
        CoffeeSpecification.nameContains("Veracruz")
            .and(CoffeeSpecification.hasOrigin("Veracruz"))
            .and(CoffeeSpecification.hasRoastLevel("Medio"))
            .and(CoffeeSpecification.hasProcess("Lavado"));

    List<Coffee> result = coffeeRepository.findAll(specification);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getName()).isEqualTo("Mezcla Veracruz");
    assertThat(result.getFirst().getOrigin()).isEqualTo("Veracruz");
    assertThat(result.getFirst().getRoastLevel()).isEqualTo("Medio");
    assertThat(result.getFirst().getProcess()).isEqualTo("Lavado");
  }

  @Test
  void search_shouldReturnAllCoffees_whenFiltersAreNullOrBlank() {
    Coffee veracruz =
        Coffee.builder()
            .name("Mezcla Veracruz")
            .brand("Café local")
            .origin("Veracruz")
            .roastLevel("Medio")
            .process("Lavado")
            .build();

    Coffee chiapas =
        Coffee.builder()
            .name("Chiapas Blend")
            .brand("Café local")
            .origin("Chiapas")
            .roastLevel("Oscuro")
            .process("Natural")
            .build();

    coffeeRepository.saveAll(List.of(veracruz, chiapas));

    Specification<Coffee> specification =
        CoffeeSpecification.nameContains("")
            .and(CoffeeSpecification.hasOrigin(null))
            .and(CoffeeSpecification.hasRoastLevel(""))
            .and(CoffeeSpecification.hasProcess(null));

    List<Coffee> result = coffeeRepository.findAll(specification);

    assertThat(result).extracting(Coffee::getName).contains("Mezcla Veracruz", "Chiapas Blend");
  }

  @Test
  void hasOwner_shouldReturnOnlyOwnedCoffees() {
    User owner = persistUser("owner@brewdeck.test");
    User other = persistUser("other@brewdeck.test");
    persistCoffee("Owned", owner);
    persistCoffee("Foreign", other);

    List<Coffee> result = coffeeRepository.findAll(CoffeeSpecification.hasOwner(owner.getId()));

    assertThat(result).extracting(Coffee::getName).containsExactly("Owned");
  }

  private User persistUser(String email) {
    User user =
        User.builder()
            .email(email)
            .passwordHash("hashed-password")
            .createdAt(LocalDateTime.now())
            .build();

    return entityManager.persistAndFlush(user);
  }

  private Coffee persistCoffee(String name, User owner) {
    Coffee coffee = Coffee.builder().name(name).owner(owner).build();

    return entityManager.persistAndFlush(coffee);
  }
}
