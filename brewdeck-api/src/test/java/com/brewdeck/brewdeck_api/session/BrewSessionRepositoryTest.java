package com.brewdeck.brewdeck_api.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.common.PostgresRepositoryTest;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import java.time.LocalDateTime;
import java.util.List;
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
    User owner = persistUser("newest-first-owner@brewdeck.test");
    Recipe recipe = persistRecipe("Mezcla Veracruz AeroPress", owner);

    BrewSession olderSession =
        BrewSession.builder()
            .recipe(recipe)
            .owner(owner)
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
            .owner(owner)
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
    User owner = persistUser("specific-recipe-owner@brewdeck.test");
    Recipe aeroPressRecipe = persistRecipe("Mezcla Veracruz AeroPress", owner);
    Recipe espressoRecipe = persistRecipe("Mezcla Veracruz Espresso", owner);

    BrewSession aeroPressSession =
        BrewSession.builder()
            .recipe(aeroPressRecipe)
            .owner(owner)
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
            .owner(owner)
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
    User owner = persistUser("pagination-size-owner@brewdeck.test");
    Recipe recipe = persistRecipe("Mezcla Veracruz AeroPress", owner);

    BrewSession sessionOne =
        BrewSession.builder()
            .recipe(recipe)
            .owner(owner)
            .brewedAt(LocalDateTime.of(2026, 4, 21, 10, 0))
            .rating(9)
            .build();

    BrewSession sessionTwo =
        BrewSession.builder()
            .recipe(recipe)
            .owner(owner)
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

  @Test
  void findTop10ByRecipeIdAndRatingIsNotNull_shouldReturnOnlyRatedNewestFirstCappedAt10() {
    User owner = persistUser("top10-owner@brewdeck.test");
    Recipe recipe = persistRecipe("Mezcla Veracruz AeroPress", owner);

    // Unrated session (rating null) must be excluded.
    entityManager.persist(
        BrewSession.builder()
            .recipe(recipe)
            .owner(owner)
            .brewedAt(LocalDateTime.of(2026, 5, 1, 10, 0))
            .actualTemp(90)
            .build());

    // 11 rated sessions with ascending brewedAt: only the newest 10 come back, newest first.
    for (int i = 1; i <= 11; i++) {
      entityManager.persist(
          BrewSession.builder()
              .recipe(recipe)
              .owner(owner)
              .brewedAt(LocalDateTime.of(2026, 4, i, 10, 0))
              .actualTemp(88 + i)
              .actualTime("2:30")
              .tasteResult("taste " + i)
              .rating((i % 10) + 1)
              .build());
    }

    entityManager.flush();
    entityManager.clear();

    List<BrewSession> result =
        brewSessionRepository.findTop10ByRecipeIdAndRatingIsNotNullOrderByBrewedAtDesc(
            recipe.getId());

    assertThat(result).hasSize(10);
    assertThat(result).allSatisfy(session -> assertThat(session.getRating()).isNotNull());
    assertThat(result.get(0).getBrewedAt()).isEqualTo(LocalDateTime.of(2026, 4, 11, 10, 0));
    assertThat(result.get(0).getBrewedAt()).isAfter(result.get(1).getBrewedAt());
  }

  @Test
  void findTopRated_shouldReturnOnlyOwnersRecipe() {
    User owner = persistUser("owner@brewdeck.test");
    User other = persistUser("other@brewdeck.test");

    Recipe ownedRecipe = persistRecipe("Owned Recipe", owner);
    Recipe foreignRecipe = persistRecipe("Foreign Recipe", other);

    persistRatedSession(ownedRecipe, owner, 9);
    persistRatedSession(foreignRecipe, other, 5);

    List<TopRatedRecipe> result =
        brewSessionRepository.findTopRated(owner.getId(), PageRequest.of(0, 10));

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getRecipeName()).isEqualTo("Owned Recipe");
  }

  @Test
  void findAverageRating_shouldOnlyAverageOwnersSessions() {
    User owner = persistUser("owner-avg@brewdeck.test");
    User other = persistUser("other-avg@brewdeck.test");

    Recipe ownedRecipe = persistRecipe("Owned Avg Recipe", owner);
    Recipe foreignRecipe = persistRecipe("Foreign Avg Recipe", other);

    persistRatedSession(ownedRecipe, owner, 8);
    persistRatedSession(foreignRecipe, other, 2);

    Double result = brewSessionRepository.findAverageRating(owner.getId());

    assertThat(result).isEqualTo(8.0);
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

  private BrewSession persistRatedSession(Recipe recipe, User owner, int rating) {
    BrewSession session =
        BrewSession.builder()
            .recipe(recipe)
            .owner(owner)
            .brewedAt(LocalDateTime.now())
            .rating(rating)
            .build();

    return entityManager.persistAndFlush(session);
  }

  private Recipe persistRecipe(String recipeName, User owner) {
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
            .owner(owner)
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
            .owner(owner)
            .build();

    return entityManager.persistAndFlush(recipe);
  }
}
