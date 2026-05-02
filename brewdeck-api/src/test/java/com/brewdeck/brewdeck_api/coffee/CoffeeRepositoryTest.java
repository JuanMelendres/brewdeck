package com.brewdeck.brewdeck_api.coffee;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.common.PostgresRepositoryTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class CoffeeRepositoryTest extends PostgresRepositoryTest {

  @Autowired private CoffeeRepository coffeeRepository;

  @Test
  void save_shouldPersistCoffee() {
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
            .acidity("Media")
            .body("Medio")
            .sweetness("Media")
            .bitterness("Baja")
            .description("Clean and aromatic coffee.")
            .build();

    Coffee savedCoffee = coffeeRepository.save(coffee);

    assertThat(savedCoffee.getId()).isNotNull();
    assertThat(savedCoffee.getName()).isEqualTo("Mezcla Veracruz");
    assertThat(savedCoffee.getCreatedAt()).isNotNull();
  }

  @Test
  void findById_shouldReturnCoffee_whenCoffeeExists() {
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
            .acidity("Media")
            .body("Medio")
            .sweetness("Alta")
            .bitterness("Baja")
            .description("Balanced coffee for daily brewing.")
            .build();

    Coffee savedCoffee = coffeeRepository.save(coffee);

    Optional<Coffee> result = coffeeRepository.findById(savedCoffee.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("Mezcla Maya");
    assertThat(result.get().getNotesPrimary()).isEqualTo("Chocolate");
  }

  @Test
  void findAll_shouldReturnAllCoffees() {
    Coffee coffeeOne =
        Coffee.builder()
            .name("Mezcla Veracruz")
            .brand("Café local")
            .origin("Veracruz")
            .roastLevel("Medio")
            .notesPrimary("Cardamomo")
            .build();

    Coffee coffeeTwo =
        Coffee.builder()
            .name("Mezcla Maya")
            .brand("Café local")
            .origin("México")
            .roastLevel("Medio")
            .notesPrimary("Chocolate")
            .build();

    coffeeRepository.save(coffeeOne);
    coffeeRepository.save(coffeeTwo);

    assertThat(coffeeRepository.findAll()).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void delete_shouldRemoveCoffee() {
    Coffee coffee =
        Coffee.builder().name("Coffee to Delete").brand("Test brand").origin("Test origin").build();

    Coffee savedCoffee = coffeeRepository.save(coffee);

    coffeeRepository.deleteById(savedCoffee.getId());

    Optional<Coffee> result = coffeeRepository.findById(savedCoffee.getId());

    assertThat(result).isEmpty();
  }
}
