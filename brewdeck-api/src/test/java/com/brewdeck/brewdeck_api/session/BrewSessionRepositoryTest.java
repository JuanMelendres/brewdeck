package com.brewdeck.brewdeck_api.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.common.PostgresRepositoryTest;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class BrewSessionRepositoryTest extends PostgresRepositoryTest {

  @Autowired private BrewSessionRepository brewSessionRepository;

  @Autowired
  private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

  @Test
  void findByRecipeIdOrderByBrewedAtDesc_shouldReturnSessionsOrderedByNewestFirst() {
    Recipe recipe = persistRecipe("Mezcla Veracruz AeroPress");

    BrewSession olderSession =
        BrewSession.builder()
            .recipe(recipe)
            .brewedAt(LocalDateTime.of(2026, 4, 20, 10, 0))
            .actualGrind("Timemore S3 - 5.5")
            .actualTemp(90)
            .actualTime("2:30")
            .tasteResult("Balanced")
            .rating(8)
            .adjustmentNotes("Good result.")
            .build();

    BrewSession newerSession =
        BrewSession.builder()
            .recipe(recipe)
            .brewedAt(LocalDateTime.of(2026, 4, 21, 10, 0))
            .actualGrind("Timemore S3 - 5.5")
            .actualTemp(91)
            .actualTime("2:20")
            .tasteResult("More aromatic")
            .rating(9)
            .adjustmentNotes("Repeat this recipe.")
            .build();

    entityManager.persist(olderSession);
    entityManager.persist(newerSession);
    entityManager.flush();
    entityManager.clear();

    List<BrewSession> result =
        brewSessionRepository.findByRecipeIdOrderByBrewedAtDesc(recipe.getId());

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getRating()).isEqualTo(9);
    assertThat(result.get(0).getTasteResult()).isEqualTo("More aromatic");
    assertThat(result.get(1).getRating()).isEqualTo(8);
    assertThat(result.get(1).getTasteResult()).isEqualTo("Balanced");
  }

  @Test
  void findByRecipeIdOrderByBrewedAtDesc_shouldReturnOnlySessionsForSpecificRecipe() {
    Recipe aeroPressRecipe = persistRecipe("Mezcla Veracruz AeroPress");
    Recipe espressoRecipe = persistRecipe("Mezcla Veracruz Espresso");

    BrewSession aeroPressSession =
        BrewSession.builder()
            .recipe(aeroPressRecipe)
            .brewedAt(LocalDateTime.of(2026, 4, 21, 10, 0))
            .actualGrind("Timemore S3 - 5.5")
            .actualTemp(90)
            .actualTime("2:30")
            .tasteResult("Balanced")
            .rating(9)
            .adjustmentNotes("Repeat.")
            .build();

    BrewSession espressoSession =
        BrewSession.builder()
            .recipe(espressoRecipe)
            .brewedAt(LocalDateTime.of(2026, 4, 21, 11, 0))
            .actualGrind("Fine")
            .actualTemp(93)
            .actualTime("28s")
            .tasteResult("Strong body")
            .rating(8)
            .adjustmentNotes("Try a little coarser.")
            .build();

    entityManager.persist(aeroPressSession);
    entityManager.persist(espressoSession);
    entityManager.flush();
    entityManager.clear();

    List<BrewSession> result =
        brewSessionRepository.findByRecipeIdOrderByBrewedAtDesc(aeroPressRecipe.getId());

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getRecipe().getId()).isEqualTo(aeroPressRecipe.getId());
    assertThat(result.getFirst().getTasteResult()).isEqualTo("Balanced");
  }

  private Recipe persistRecipe(String recipeName) {
    Coffee coffee =
        Coffee.builder()
            .name("Mezcla Veracruz")
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

    BrewMethod method =
        BrewMethod.builder()
            .name(recipeName.contains("Espresso") ? "Espresso" : "AeroPress")
            .description("Brew method created for repository tests.")
            .build();

    Coffee persistedCoffee = entityManager.persistAndFlush(coffee);
    BrewMethod persistedMethod = entityManager.persistAndFlush(method);

    Recipe recipe =
        Recipe.builder()
            .coffee(persistedCoffee)
            .method(persistedMethod)
            .name(recipeName)
            .ratio("1:15")
            .grindSetting("Timemore S3 - 5.5")
            .waterTemp(90)
            .brewTime("2:30")
            .steps("Bloom 30s, stir gently, press slowly.")
            .expectedTaste("Clean and aromatic.")
            .favorite(false)
            .build();

    return entityManager.persistAndFlush(recipe);
  }
}
