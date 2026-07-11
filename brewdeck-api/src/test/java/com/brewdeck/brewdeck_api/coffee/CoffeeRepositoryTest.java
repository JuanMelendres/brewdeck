package com.brewdeck.brewdeck_api.coffee;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.common.PostgresRepositoryTest;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class CoffeeRepositoryTest extends PostgresRepositoryTest {

  @Autowired private CoffeeRepository coffeeRepository;

  @Autowired private TestEntityManager entityManager;

  @Test
  void save_shouldPersistCoffee() {
    User owner = persistUser("save-owner@brewdeck.test");
    Coffee coffee =
        Coffee.builder()
            .name("Mezcla Veracruz")
            .brand("Café local")
            .origin("Veracruz")
            .region("Coatepec")
            .variety("Blend")
            .process("Lavado")
            .roastLevel("Medio")
            .notesPrimary("Cardamomo")
            .notesSecondary("Canela, clavo")
            .acidityScore(3)
            .bodyScore(3)
            .sweetnessScore(4)
            .bitternessScore(2)
            .description("Clean and aromatic coffee.")
            .owner(owner)
            .build();

    Coffee savedCoffee = coffeeRepository.save(coffee);

    assertThat(savedCoffee.getId()).isNotNull();
    assertThat(savedCoffee.getName()).isEqualTo("Mezcla Veracruz");
    assertThat(savedCoffee.getCreatedAt()).isNotNull();
  }

  @Test
  void findById_shouldReturnCoffee_whenCoffeeExists() {
    User owner = persistUser("find-by-id-owner@brewdeck.test");
    Coffee coffee =
        Coffee.builder()
            .name("Mezcla Maya")
            .brand("Café local")
            .origin("México")
            .variety("Blend")
            .process("Lavado")
            .roastLevel("Medio")
            .notesPrimary("Chocolate")
            .notesSecondary("Nuez, caramelo")
            .acidityScore(3)
            .bodyScore(3)
            .sweetnessScore(4)
            .bitternessScore(2)
            .description("Balanced coffee for daily brewing.")
            .owner(owner)
            .build();

    Coffee savedCoffee = coffeeRepository.save(coffee);

    Optional<Coffee> result = coffeeRepository.findById(savedCoffee.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("Mezcla Maya");
    assertThat(result.get().getNotesPrimary()).isEqualTo("Chocolate");
  }

  @Test
  void findAll_shouldReturnAllCoffees() {
    User owner = persistUser("find-all-owner@brewdeck.test");
    Coffee coffeeOne =
        Coffee.builder()
            .name("Mezcla Veracruz")
            .brand("Café local")
            .origin("Veracruz")
            .roastLevel("Medio")
            .notesPrimary("Cardamomo")
            .owner(owner)
            .build();

    Coffee coffeeTwo =
        Coffee.builder()
            .name("Mezcla Maya")
            .brand("Café local")
            .origin("México")
            .roastLevel("Medio")
            .notesPrimary("Chocolate")
            .owner(owner)
            .build();

    coffeeRepository.save(coffeeOne);
    coffeeRepository.save(coffeeTwo);

    assertThat(coffeeRepository.findAll()).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void delete_shouldRemoveCoffee() {
    User owner = persistUser("delete-owner@brewdeck.test");
    Coffee coffee =
        Coffee.builder()
            .name("Coffee to Delete")
            .brand("Test brand")
            .origin("Test origin")
            .owner(owner)
            .build();

    Coffee savedCoffee = coffeeRepository.save(coffee);

    coffeeRepository.deleteById(savedCoffee.getId());

    Optional<Coffee> result = coffeeRepository.findById(savedCoffee.getId());

    assertThat(result).isEmpty();
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
}
