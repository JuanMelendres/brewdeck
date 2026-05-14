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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class BrewSessionSpecificationRepositoryTest extends PostgresRepositoryTest {

  @Autowired private BrewSessionRepository brewSessionRepository;

  @Autowired
  private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

  @Test
  void search_shouldFilterByRecipeIdAndRating() {
    Recipe recipe = persistRecipe("Mezcla Veracruz AeroPress");
    Recipe otherRecipe = persistRecipe("Mezcla Veracruz V60");

    BrewSession matchingSession =
        BrewSession.builder()
            .recipe(recipe)
            .brewedAt(LocalDateTime.now())
            .rating(9)
            .tasteResult("Balanced")
            .build();

    BrewSession otherSession =
        BrewSession.builder()
            .recipe(otherRecipe)
            .brewedAt(LocalDateTime.now())
            .rating(7)
            .tasteResult("Weak")
            .build();

    entityManager.persist(matchingSession);
    entityManager.persist(otherSession);
    entityManager.flush();
    entityManager.clear();

    Specification<BrewSession> specification =
        BrewSessionSpecification.hasRecipeId(recipe.getId())
            .and(BrewSessionSpecification.hasRating(9));

    List<BrewSession> result = brewSessionRepository.findAll(specification);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getRecipe().getId()).isEqualTo(recipe.getId());
    assertThat(result.getFirst().getRating()).isEqualTo(9);
    assertThat(result.getFirst().getTasteResult()).isEqualTo("Balanced");
  }

  @Test
  void search_shouldReturnAllSessions_whenFiltersAreNull() {
    Recipe recipe = persistRecipe("Mezcla Veracruz AeroPress");

    BrewSession sessionOne =
        BrewSession.builder().recipe(recipe).brewedAt(LocalDateTime.now()).rating(9).build();

    BrewSession sessionTwo =
        BrewSession.builder().recipe(recipe).brewedAt(LocalDateTime.now()).rating(8).build();

    entityManager.persist(sessionOne);
    entityManager.persist(sessionTwo);
    entityManager.flush();
    entityManager.clear();

    Specification<BrewSession> specification =
        BrewSessionSpecification.hasRecipeId(null).and(BrewSessionSpecification.hasRating(null));

    List<BrewSession> result = brewSessionRepository.findAll(specification);

    assertThat(result).hasSizeGreaterThanOrEqualTo(2);
    assertThat(result).extracting(BrewSession::getRating).contains(9, 8);
  }

  private Recipe persistRecipe(String recipeName) {
    Coffee coffee =
        Coffee.builder()
            .name("Mezcla Veracruz " + System.nanoTime())
            .brand("Café local")
            .origin("Veracruz")
            .roastLevel("Medio")
            .process("Lavado")
            .build();

    BrewMethod method =
        BrewMethod.builder()
            .name("Test Method " + recipeName + " " + System.nanoTime())
            .description("Brew method created for specification tests.")
            .build();

    Coffee persistedCoffee = entityManager.persistAndFlush(coffee);
    BrewMethod persistedMethod = entityManager.persistAndFlush(method);

    Recipe recipe =
        Recipe.builder()
            .coffee(persistedCoffee)
            .method(persistedMethod)
            .name(recipeName)
            .favorite(false)
            .build();

    return entityManager.persistAndFlush(recipe);
  }
}
