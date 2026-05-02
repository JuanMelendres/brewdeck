package com.brewdeck.brewdeck_api.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.common.PostgresRepositoryTest;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RecipeRepositoryTest extends PostgresRepositoryTest {

  @Autowired private RecipeRepository recipeRepository;

  @Autowired
  private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

  @Test
  void findByFavoriteTrue_shouldReturnOnlyFavoriteRecipes() {
    Coffee coffee = persistCoffee("Mezcla Veracruz");
    BrewMethod method = persistBrewMethod("AeroPress");

    Recipe favoriteRecipe =
        Recipe.builder()
            .coffee(coffee)
            .method(method)
            .name("Favorite AeroPress Recipe")
            .coffeeGrams(BigDecimal.valueOf(15))
            .waterGrams(BigDecimal.valueOf(230))
            .ratio("1:15")
            .grindSetting("Timemore S3 - 5.5")
            .waterTemp(90)
            .brewTime("2:30")
            .steps("Bloom 30s, stir gently, press slowly.")
            .expectedTaste("Clean and aromatic.")
            .favorite(true)
            .build();

    Recipe nonFavoriteRecipe =
        Recipe.builder()
            .coffee(coffee)
            .method(method)
            .name("Non Favorite Recipe")
            .coffeeGrams(BigDecimal.valueOf(18))
            .waterGrams(BigDecimal.valueOf(270))
            .ratio("1:15")
            .grindSetting("Medium")
            .waterTemp(92)
            .brewTime("3:00")
            .steps("Standard recipe.")
            .expectedTaste("Balanced.")
            .favorite(false)
            .build();

    entityManager.persist(favoriteRecipe);
    entityManager.persist(nonFavoriteRecipe);
    entityManager.flush();
    entityManager.clear();

    List<Recipe> result = recipeRepository.findByFavoriteTrue();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getName()).isEqualTo("Favorite AeroPress Recipe");
    assertThat(result.getFirst().getFavorite()).isTrue();
  }

  @Test
  void findByCoffeeId_shouldReturnRecipesForSpecificCoffee() {
    Coffee veracruz = persistCoffee("Mezcla Veracruz");
    Coffee maya = persistCoffee("Mezcla Maya");

    BrewMethod method = persistBrewMethod("V60");

    Recipe veracruzRecipe =
        Recipe.builder()
            .coffee(veracruz)
            .method(method)
            .name("Veracruz V60")
            .favorite(false)
            .build();

    Recipe mayaRecipe =
        Recipe.builder().coffee(maya).method(method).name("Maya V60").favorite(false).build();

    entityManager.persist(veracruzRecipe);
    entityManager.persist(mayaRecipe);
    entityManager.flush();
    entityManager.clear();

    List<Recipe> result = recipeRepository.findByCoffeeId(veracruz.getId());

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getName()).isEqualTo("Veracruz V60");
    assertThat(result.getFirst().getCoffee().getId()).isEqualTo(veracruz.getId());
  }

  @Test
  void findByMethodId_shouldReturnRecipesForSpecificMethod() {
    Coffee coffee = persistCoffee("Mezcla Veracruz");

    BrewMethod aeroPress = persistBrewMethod("AeroPress");
    BrewMethod espresso = persistBrewMethod("Espresso");

    Recipe aeroPressRecipe =
        Recipe.builder()
            .coffee(coffee)
            .method(aeroPress)
            .name("Veracruz AeroPress")
            .favorite(false)
            .build();

    Recipe espressoRecipe =
        Recipe.builder()
            .coffee(coffee)
            .method(espresso)
            .name("Veracruz Espresso")
            .favorite(false)
            .build();

    entityManager.persist(aeroPressRecipe);
    entityManager.persist(espressoRecipe);
    entityManager.flush();
    entityManager.clear();

    List<Recipe> result = recipeRepository.findByMethodId(aeroPress.getId());

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getName()).isEqualTo("Veracruz AeroPress");
    assertThat(result.getFirst().getMethod().getId()).isEqualTo(aeroPress.getId());
  }

  private Coffee persistCoffee(String name) {
    Coffee coffee =
        Coffee.builder()
            .name(name)
            .brand("Café local")
            .origin("Veracruz")
            .variety("Blend")
            .process("Lavado")
            .roastLevel("Medio")
            .notesPrimary("Cardamomo")
            .notesSecondary("Canela, clavo")
            .acidity("Media")
            .body("Medio")
            .sweetness("Media")
            .bitterness("Baja")
            .description("Coffee created for repository tests.")
            .build();

    return entityManager.persistAndFlush(coffee);
  }

  private BrewMethod persistBrewMethod(String name) {
    BrewMethod method =
        BrewMethod.builder()
            .name(name)
            .description("Brew method created for repository tests.")
            .build();

    return entityManager.persistAndFlush(method);
  }
}
