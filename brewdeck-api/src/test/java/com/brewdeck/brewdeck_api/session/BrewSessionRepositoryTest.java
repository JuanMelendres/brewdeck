package com.brewdeck.brewdeck_api.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.common.PostgresRepositoryTest;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class BrewSessionRepositoryTest extends PostgresRepositoryTest {

  @Autowired private BrewSessionRepository brewSessionRepository;

  @Autowired
  private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

  @Test
  void findByRecipeIdOrderByBrewedAtDesc_shouldReturnPagedSessionsOrderedByNewestFirst() {
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

    Pageable pageable = PageRequest.of(0, 10);

    Page<BrewSession> result =
        brewSessionRepository.findByRecipeIdOrderByBrewedAtDesc(recipe.getId(), pageable);

    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent().get(0).getRating()).isEqualTo(9);
    assertThat(result.getContent().get(0).getTasteResult()).isEqualTo("More aromatic");
    assertThat(result.getContent().get(1).getRating()).isEqualTo(8);
    assertThat(result.getContent().get(1).getTasteResult()).isEqualTo("Balanced");
    assertThat(result.getTotalElements()).isEqualTo(2);
  }

  @Test
  void findByRecipeIdOrderByBrewedAtDesc_shouldReturnPagedSessionsForSpecificRecipe() {
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

    Pageable pageable = PageRequest.of(0, 10);

    Page<BrewSession> result =
        brewSessionRepository.findByRecipeIdOrderByBrewedAtDesc(aeroPressRecipe.getId(), pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().getRecipe().getId())
        .isEqualTo(aeroPressRecipe.getId());
    assertThat(result.getContent().getFirst().getTasteResult()).isEqualTo("Balanced");
    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  void findByRecipeIdOrderByBrewedAtDesc_shouldRespectPaginationSize() {
    Recipe recipe = persistRecipe("Mezcla Veracruz AeroPress");

    BrewSession sessionOne =
        BrewSession.builder()
            .recipe(recipe)
            .brewedAt(LocalDateTime.of(2026, 4, 21, 10, 0))
            .rating(9)
            .build();

    BrewSession sessionTwo =
        BrewSession.builder()
            .recipe(recipe)
            .brewedAt(LocalDateTime.of(2026, 4, 22, 10, 0))
            .rating(10)
            .build();

    entityManager.persist(sessionOne);
    entityManager.persist(sessionTwo);
    entityManager.flush();
    entityManager.clear();

    Pageable pageable = PageRequest.of(0, 1);

    Page<BrewSession> result =
        brewSessionRepository.findByRecipeIdOrderByBrewedAtDesc(recipe.getId(), pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getTotalPages()).isEqualTo(2);
    assertThat(result.getSize()).isEqualTo(1);
    assertThat(result.getContent().getFirst().getRating()).isEqualTo(10);
  }

  private Recipe persistRecipe(String recipeName) {
    Coffee coffee =
        Coffee.builder()
            .name("Mezcla Veracruz " + System.nanoTime())
            .brand("Café local")
            .origin("Veracruz")
            .variety("Blend")
            .process("Lavado")
            .roastLevel("Medio")
            .notesPrimary("Cardamomo")
            .notesSecondary("Canela, clavo")
            .acidityScore(3)
            .bodyScore(3)
            .sweetnessScore(3)
            .bitternessScore(2)
            .description("Coffee created for repository tests.")
            .build();

    BrewMethod method =
        BrewMethod.builder()
            .name("Test Method " + recipeName + " " + System.nanoTime())
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
