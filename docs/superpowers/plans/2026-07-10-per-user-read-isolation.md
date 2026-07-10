# Per-User Read Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scope every read (lists, by-id, favorites, recipe stats, analytics, dashboard) to the authenticated owner and enforce `owner_id NOT NULL`.

**Architecture:** Reuse `CurrentUserProvider.require().getId()` as `ownerId`. Add `hasOwner` predicates to the three Specifications, owner-scoped derived finders (`...AndOwnerId`) and `@Query` owner params to the repositories, and route each service's reads through them. `brew_methods` stay global; public-share stays token-based. A final Flyway V7 tightens `owner_id` to `NOT NULL`.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, Flyway, PostgreSQL 16, JUnit 5, Mockito, MockMvc, Testcontainers, AssertJ.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-07-10-per-user-read-isolation-design.md`.
- Owned resources: `coffees`, `recipes`, `brew_sessions`. `brew_methods` is shared/global — never owner-filter it (except its usage analytics, which filters the joined recipes).
- Public share endpoints (`findByShareToken`, share/unshare) stay cross-user — do not owner-scope them.
- Cross-user access to a row by id → `EntityNotFoundException` → 404 (reuse existing path; no new exception).
- `ownerId` = `currentUserProvider.require().getId()`. Add a private `currentOwnerId()` helper in each service that needs it.
- Collection GETs keep returning `PageResponse<T>`; bounded analytics keep returning `List<T>`. No API shape/status changes.
- Bean Validation messages: no special symbols (write `degrees Celsius`, not the symbol).
- Verify per task: `sh mvnw -Dtest=<Test> test`. Before finishing: `sh mvnw spotless:apply && sh mvnw clean verify && sh mvnw pmd:check`.
- Commit style: Conventional Commits, scope `api`. Every commit ends with the two trailers used on this branch (Co-Authored-By + Claude-Session).
- Branch: `feature/auth-ownership-reads` (already created).

---

### Task 1: Coffee owner-scoped reads

**Files:**
- Modify: `src/main/java/com/brewdeck/brewdeck_api/coffee/CoffeeSpecification.java`
- Modify: `src/main/java/com/brewdeck/brewdeck_api/coffee/CoffeeRepository.java`
- Modify: `src/main/java/com/brewdeck/brewdeck_api/coffee/CoffeeService.java`
- Test: `src/test/java/com/brewdeck/brewdeck_api/coffee/CoffeeSpecificationRepositoryTest.java`
- Test: `src/test/java/com/brewdeck/brewdeck_api/coffee/CoffeeServiceTest.java`

**Interfaces:**
- Produces: `CoffeeSpecification.hasOwner(Long ownerId)`; `CoffeeRepository.findByIdAndOwnerId(Long, Long)`, `existsByIdAndOwnerId(Long, Long)`, `countByOwnerId(Long)`.
- Consumes: `CurrentUserProvider.require()` (from B.1, already a field on `CoffeeService`).

- [ ] **Step 1: Add the failing spec test**

In `CoffeeSpecificationRepositoryTest.java`, persist an owner and a second user (via the injected `TestEntityManager`/repositories the test already uses), give two coffees different owners, then:

```java
@Test
void hasOwner_shouldReturnOnlyOwnedCoffees() {
  User owner = persistUser("owner@brewdeck.test");
  User other = persistUser("other@brewdeck.test");
  persistCoffee("Owned", owner);
  persistCoffee("Foreign", other);

  List<Coffee> result =
      coffeeRepository.findAll(CoffeeSpecification.hasOwner(owner.getId()));

  assertThat(result).extracting(Coffee::getName).containsExactly("Owned");
}
```

Add `persistUser`/`persistCoffee` helpers if the class lacks them (persist a `User` with a non-null `passwordHash` and `createdAt`, and set `owner` on the coffee).

- [ ] **Step 2: Run it, verify it fails**

Run: `sh mvnw -Dtest=CoffeeSpecificationRepositoryTest test`
Expected: FAIL — `hasOwner` does not exist (compile error).

- [ ] **Step 3: Add `hasOwner` to the specification**

In `CoffeeSpecification.java`:

```java
public static Specification<Coffee> hasOwner(Long ownerId) {
  return (root, query, criteriaBuilder) ->
      ownerId == null
          ? criteriaBuilder.disjunction()
          : criteriaBuilder.equal(root.get("owner").get("id"), ownerId);
}
```

(Null owner id → `disjunction()` (match nothing) — a missing current user must never widen the result set.)

- [ ] **Step 4: Add the owner-scoped repository finders**

In `CoffeeRepository.java`:

```java
Optional<Coffee> findByIdAndOwnerId(Long id, Long ownerId);

boolean existsByIdAndOwnerId(Long id, Long ownerId);

long countByOwnerId(Long ownerId);
```

- [ ] **Step 5: Route `CoffeeService` reads through owner scoping**

In `CoffeeService.java` add the helper and apply it. `search` gains `.and(CoffeeSpecification.hasOwner(currentOwnerId()))`; `findById`/`update` use `findByIdAndOwnerId`; `delete` uses `existsByIdAndOwnerId`:

```java
private Long currentOwnerId() {
  return currentUserProvider.require().getId();
}
```

```java
public PageResponse<CoffeeResponse> search(CoffeeFilter filter, Pageable pageable) {
  return PageResponse.fromPage(
      coffeeRepository
          .findAll(
              CoffeeSpecification.nameContains(filter.name())
                  .and(CoffeeSpecification.hasOrigin(filter.origin()))
                  .and(CoffeeSpecification.hasRoastLevel(filter.roastLevel()))
                  .and(CoffeeSpecification.hasProcess(filter.process()))
                  .and(CoffeeSpecification.hasOwner(currentOwnerId())),
              pageable)
          .map(CoffeeResponse::fromEntity));
}
```

```java
public CoffeeResponse findById(Long id) {
  Coffee coffee =
      coffeeRepository
          .findByIdAndOwnerId(id, currentOwnerId())
          .orElseThrow(() -> new EntityNotFoundException("Coffee not found"));
  return CoffeeResponse.fromEntity(coffee);
}
```

Apply the same `findByIdAndOwnerId` change in `update`, and change `delete`'s guard to `if (!coffeeRepository.existsByIdAndOwnerId(id, currentOwnerId()))`.

- [ ] **Step 6: Update `CoffeeServiceTest` stubs + add not-owned case**

The service now calls `currentOwnerId()` (stub `currentUserProvider.require()` → a `User` with an id) in every read test, and by-id tests stub `findByIdAndOwnerId(id, ownerId)` / `existsByIdAndOwnerId`. Add:

```java
@Test
void findById_shouldThrow_whenCoffeeOwnedByAnotherUser() {
  when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
  when(coffeeRepository.findByIdAndOwnerId(99L, 42L)).thenReturn(Optional.empty());

  assertThatThrownBy(() -> coffeeService.findById(99L))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessage("Coffee not found");
}
```

Update existing `findById`/`update`/`delete`/`search` tests to stub `currentUserProvider.require()` and the `...AndOwnerId` methods instead of `findById`/`existsById`.

- [ ] **Step 7: Run the coffee tests**

Run: `sh mvnw -Dtest=CoffeeServiceTest,CoffeeSpecificationRepositoryTest test`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/brewdeck/brewdeck_api/coffee src/test/java/com/brewdeck/brewdeck_api/coffee
git commit -m "feat(api): scope coffee reads to the current owner"
```

---

### Task 2: Recipe owner-scoped reads

**Files:**
- Modify: `src/main/java/com/brewdeck/brewdeck_api/recipe/RecipeSpecification.java`
- Modify: `src/main/java/com/brewdeck/brewdeck_api/recipe/RecipeRepository.java`
- Modify: `src/main/java/com/brewdeck/brewdeck_api/recipe/RecipeService.java`
- Test: `src/test/java/com/brewdeck/brewdeck_api/recipe/RecipeRepositoryTest.java`
- Test: `src/test/java/com/brewdeck/brewdeck_api/recipe/RecipeSpecificationRepositoryTest.java`
- Test: `src/test/java/com/brewdeck/brewdeck_api/recipe/RecipeServiceTest.java`

**Interfaces:**
- Produces: `RecipeSpecification.hasOwner(Long)`; `RecipeRepository.findByIdAndOwnerId(Long, Long)`, `existsByIdAndOwnerId(Long, Long)`, `countByOwnerId(Long)`, `findByFavoriteTrueAndOwnerId(Long, Pageable)`, `countByFavoriteTrueAndOwnerId(Long)`, `findByCoffeeIdAndOwnerId(Long, Long, Pageable)`, `findByMethodIdAndOwnerId(Long, Long, Pageable)`.
- Consumes: `CurrentUserProvider` (already a field on `RecipeService`).

- [ ] **Step 1: Add failing repository test for favorites-by-owner**

In `RecipeRepositoryTest.java`:

```java
@Test
void findByFavoriteTrueAndOwnerId_shouldReturnOnlyOwnersFavorites() {
  User owner = persistUser("owner@brewdeck.test");
  User other = persistUser("other@brewdeck.test");
  persistFavoriteRecipe("Owned fav", owner);
  persistFavoriteRecipe("Foreign fav", other);

  Page<Recipe> result =
      recipeRepository.findByFavoriteTrueAndOwnerId(owner.getId(), PageRequest.of(0, 10));

  assertThat(result.getContent()).extracting(Recipe::getName).containsExactly("Owned fav");
}
```

Add `persistUser` and `persistFavoriteRecipe(name, owner)` helpers (persist coffee + method + a favorite recipe with `owner` set).

- [ ] **Step 2: Run it, verify it fails**

Run: `sh mvnw -Dtest=RecipeRepositoryTest test`
Expected: FAIL — method does not exist (compile error).

- [ ] **Step 3: Add `hasOwner` to the specification**

In `RecipeSpecification.java`:

```java
public static Specification<Recipe> hasOwner(Long ownerId) {
  return (root, query, criteriaBuilder) ->
      ownerId == null
          ? criteriaBuilder.disjunction()
          : criteriaBuilder.equal(root.get("owner").get("id"), ownerId);
}
```

- [ ] **Step 4: Add owner-scoped repository finders**

In `RecipeRepository.java`:

```java
@EntityGraph(attributePaths = {"coffee", "method"})
Optional<Recipe> findByIdAndOwnerId(Long id, Long ownerId);

boolean existsByIdAndOwnerId(Long id, Long ownerId);

long countByOwnerId(Long ownerId);

@EntityGraph(attributePaths = {"coffee", "method"})
Page<Recipe> findByFavoriteTrueAndOwnerId(Long ownerId, Pageable pageable);

long countByFavoriteTrueAndOwnerId(Long ownerId);

@EntityGraph(attributePaths = {"coffee", "method"})
Page<Recipe> findByCoffeeIdAndOwnerId(Long coffeeId, Long ownerId, Pageable pageable);

@EntityGraph(attributePaths = {"coffee", "method"})
Page<Recipe> findByMethodIdAndOwnerId(Long methodId, Long ownerId, Pageable pageable);
```

- [ ] **Step 5: Route `RecipeService` reads through owner scoping**

Add the helper and apply it. `search` `.and(RecipeSpecification.hasOwner(currentOwnerId()))`. `findRecipeById` becomes owner-scoped (this centralizes get/update/favorite/unfavorite). `delete` uses `existsByIdAndOwnerId`. `findFavorites`/`findByCoffeeId`/`findByMethodId` use the `...AndOwnerId` finders. `create`'s coffee lookup becomes owner-scoped; the method lookup stays global.

```java
private Long currentOwnerId() {
  return currentUserProvider.require().getId();
}

private Recipe findRecipeById(Long id) {
  return recipeRepository
      .findByIdAndOwnerId(id, currentOwnerId())
      .orElseThrow(() -> new EntityNotFoundException(RECIPE_NOT_FOUND));
}

private Coffee findCoffeeById(Long id) {
  return coffeeRepository
      .findByIdAndOwnerId(id, currentOwnerId())
      .orElseThrow(() -> new EntityNotFoundException(COFFEE_NOT_FOUND));
}
```

```java
public PageResponse<RecipeResponse> findFavorites(Pageable pageable) {
  return PageResponse.fromPage(
      recipeRepository
          .findByFavoriteTrueAndOwnerId(currentOwnerId(), pageable)
          .map(RecipeResponse::fromEntity));
}

public PageResponse<RecipeResponse> findByCoffeeId(Long coffeeId, Pageable pageable) {
  return PageResponse.fromPage(
      recipeRepository
          .findByCoffeeIdAndOwnerId(coffeeId, currentOwnerId(), pageable)
          .map(RecipeResponse::fromEntity));
}

public PageResponse<RecipeResponse> findByMethodId(Long methodId, Pageable pageable) {
  return PageResponse.fromPage(
      recipeRepository
          .findByMethodIdAndOwnerId(methodId, currentOwnerId(), pageable)
          .map(RecipeResponse::fromEntity));
}
```

In `delete`: `if (!recipeRepository.existsByIdAndOwnerId(id, currentOwnerId()))`. Update `search` to append `.and(RecipeSpecification.hasOwner(currentOwnerId()))`. `CoffeeRepository` needs `findByIdAndOwnerId` (added in Task 1) for the coffee lookup — confirm Task 1 merged first.

- [ ] **Step 6: Update `RecipeServiceTest` + add cross-user 404 case**

Stub `currentUserProvider.require()` → a `User` with an id in every read/create/update/favorite test; swap `findById`→`findByIdAndOwnerId`, `existsById`→`existsByIdAndOwnerId`, `findByFavoriteTrue`→`findByFavoriteTrueAndOwnerId`, `findByCoffeeId`→`findByCoffeeIdAndOwnerId`, `findByMethodId`→`findByMethodIdAndOwnerId`, and the coffee lookup in create/update to `coffeeRepository.findByIdAndOwnerId`. Add:

```java
@Test
void findById_shouldThrow_whenRecipeOwnedByAnotherUser() {
  when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
  when(recipeRepository.findByIdAndOwnerId(99L, 42L)).thenReturn(Optional.empty());

  assertThatThrownBy(() -> recipeService.findById(99L))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessage("Recipe not found");
}
```

Update `RecipeSpecificationRepositoryTest` to persist owners and assert `hasOwner` filters (mirror Task 1 Step 1).

- [ ] **Step 7: Run the recipe tests**

Run: `sh mvnw -Dtest=RecipeServiceTest,RecipeRepositoryTest,RecipeSpecificationRepositoryTest test`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/brewdeck/brewdeck_api/recipe src/test/java/com/brewdeck/brewdeck_api/recipe
git commit -m "feat(api): scope recipe reads and favorites to the current owner"
```

---

### Task 3: Brew-session owner-scoped reads

**Files:**
- Modify: `src/main/java/com/brewdeck/brewdeck_api/session/BrewSessionSpecification.java`
- Modify: `src/main/java/com/brewdeck/brewdeck_api/session/BrewSessionRepository.java`
- Modify: `src/main/java/com/brewdeck/brewdeck_api/session/BrewSessionService.java`
- Test: `src/test/java/com/brewdeck/brewdeck_api/session/BrewSessionSpecificationRepositoryTest.java`
- Test: `src/test/java/com/brewdeck/brewdeck_api/session/BrewSessionServiceTest.java`

**Interfaces:**
- Produces: `BrewSessionSpecification.hasOwner(Long)`; `BrewSessionRepository.findByIdAndOwnerId(Long, Long)`, `existsByIdAndOwnerId(Long, Long)`, `countByOwnerId(Long)`.
- Consumes: `RecipeRepository.findByIdAndOwnerId` (Task 2), `CurrentUserProvider` (field on `BrewSessionService`).

- [ ] **Step 1: Add failing spec test**

In `BrewSessionSpecificationRepositoryTest.java`, persist two owners with a session each and assert `hasOwner(owner.getId())` returns only the owner's session (mirror Task 1 Step 1, persisting coffee+method+recipe+session with `owner` set).

- [ ] **Step 2: Run it, verify it fails**

Run: `sh mvnw -Dtest=BrewSessionSpecificationRepositoryTest test`
Expected: FAIL — `hasOwner` does not exist.

- [ ] **Step 3: Add `hasOwner` to the specification**

In `BrewSessionSpecification.java`:

```java
public static Specification<BrewSession> hasOwner(Long ownerId) {
  return (root, query, criteriaBuilder) ->
      ownerId == null
          ? criteriaBuilder.disjunction()
          : criteriaBuilder.equal(root.get("owner").get("id"), ownerId);
}
```

- [ ] **Step 4: Add owner-scoped repository finders**

In `BrewSessionRepository.java`:

```java
@EntityGraph(attributePaths = "recipe")
Optional<BrewSession> findByIdAndOwnerId(Long id, Long ownerId);

boolean existsByIdAndOwnerId(Long id, Long ownerId);

long countByOwnerId(Long ownerId);
```

- [ ] **Step 5: Route `BrewSessionService` reads through owner scoping**

Add `currentOwnerId()`. `search` `.and(BrewSessionSpecification.hasOwner(currentOwnerId()))`. `findById`/`update` (session lookup) → `findByIdAndOwnerId`. `delete` → `existsByIdAndOwnerId`. `create` and `update` recipe lookups → `recipeRepository.findByIdAndOwnerId(request.recipeId(), currentOwnerId())` so a session can only reference a recipe the caller owns:

```java
private Long currentOwnerId() {
  return currentUserProvider.require().getId();
}
```

```java
public BrewSessionResponse findById(Long id) {
  BrewSession session =
      brewSessionRepository
          .findByIdAndOwnerId(id, currentOwnerId())
          .orElseThrow(() -> new EntityNotFoundException("Brew session not found"));
  return BrewSessionResponse.fromEntity(session);
}
```

In `create`: `Recipe recipe = recipeRepository.findByIdAndOwnerId(request.recipeId(), currentOwnerId()).orElseThrow(() -> new EntityNotFoundException("Recipe not found"));`. Apply the same recipe lookup + owner-scoped session lookup in `update`, and `existsByIdAndOwnerId` in `delete`.

- [ ] **Step 6: Update `BrewSessionServiceTest`**

Stub `currentUserProvider.require()` → a `User` with an id in every read/create/update/delete test; swap `findById`→`findByIdAndOwnerId`, `existsById`→`existsByIdAndOwnerId`, and `recipeRepository.findById`→`recipeRepository.findByIdAndOwnerId`. Add a cross-user 404 case for `findById` (mirror Task 2 Step 6).

- [ ] **Step 7: Run the session tests**

Run: `sh mvnw -Dtest=BrewSessionServiceTest,BrewSessionSpecificationRepositoryTest test`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/brewdeck/brewdeck_api/session src/test/java/com/brewdeck/brewdeck_api/session
git commit -m "feat(api): scope brew-session reads to the current owner"
```

---

### Task 4: Per-user analytics + dashboard

**Files:**
- Modify: `src/main/java/com/brewdeck/brewdeck_api/recipe/RecipeRepository.java` (findMostUsedCoffees)
- Modify: `src/main/java/com/brewdeck/brewdeck_api/session/BrewSessionRepository.java` (findAverageRating, findStatsByRecipeId, findTopRated, findMostBrewed)
- Modify: `src/main/java/com/brewdeck/brewdeck_api/method/BrewMethodRepository.java` (findUsage)
- Modify: `src/main/java/com/brewdeck/brewdeck_api/coffee/CoffeeService.java` (getMostUsed)
- Modify: `src/main/java/com/brewdeck/brewdeck_api/recipe/RecipeStatsService.java`
- Modify: `src/main/java/com/brewdeck/brewdeck_api/method/BrewMethodService.java` (getUsage)
- Modify: `src/main/java/com/brewdeck/brewdeck_api/dashboard/DashboardService.java`
- Test: the matching `*ServiceTest` files + `BrewSessionRepositoryTest`

**Interfaces:**
- Produces: `findMostUsedCoffees(Long ownerId, Pageable)`, `findAverageRating(Long ownerId)`, `findStatsByRecipeId(Long recipeId, Long ownerId)`, `findTopRated(Long ownerId, Pageable)`, `findMostBrewed(Long ownerId, Pageable)`, `findUsage(Long ownerId)`.
- Consumes: `CurrentUserProvider`, `CoffeeRepository.countByOwnerId`, `RecipeRepository.countByOwnerId`/`countByFavoriteTrueAndOwnerId`, `BrewSessionRepository.countByOwnerId`.

- [ ] **Step 1: Add failing repository test for per-user top-rated**

In `BrewSessionRepositoryTest.java`, persist two owners each with a rated session, then assert `findTopRated(owner.getId(), PageRequest.of(0, 10))` returns only the owner's recipe. Do the same shape for `findAverageRating(owner.getId())`.

- [ ] **Step 2: Run it, verify it fails**

Run: `sh mvnw -Dtest=BrewSessionRepositoryTest test`
Expected: FAIL — signatures don't take `ownerId`.

- [ ] **Step 3: Add `ownerId` to the analytics queries**

`RecipeRepository.findMostUsedCoffees`:

```java
@Query(
    """
    select r.coffee.id as coffeeId, r.coffee.name as coffeeName, count(r) as recipeCount
    from Recipe r
    where r.owner.id = :ownerId
    group by r.coffee.id, r.coffee.name
    order by count(r) desc, r.coffee.name asc
    """)
List<MostUsedCoffee> findMostUsedCoffees(Long ownerId, Pageable pageable);
```

`BrewSessionRepository`:

```java
@Query("select avg(s.rating) from BrewSession s where s.rating is not null and s.owner.id = :ownerId")
Double findAverageRating(Long ownerId);

@Query(
    """
    select count(s) as totalSessions,
           avg(s.rating) as averageRating,
           max(s.brewedAt) as lastBrewedAt
    from BrewSession s
    where s.recipe.id = :recipeId and s.owner.id = :ownerId
    """)
RecipeSessionStats findStatsByRecipeId(Long recipeId, Long ownerId);

@Query(
    """
    select s.recipe.id as recipeId, s.recipe.name as recipeName,
           avg(s.rating) as averageRating, count(s) as totalSessions
    from BrewSession s
    where s.rating is not null and s.owner.id = :ownerId
    group by s.recipe.id, s.recipe.name
    order by avg(s.rating) desc
    """)
List<TopRatedRecipe> findTopRated(Long ownerId, Pageable pageable);

@Query(
    """
    select s.recipe.id as recipeId, s.recipe.name as recipeName, count(s) as totalSessions
    from BrewSession s
    where s.owner.id = :ownerId
    group by s.recipe.id, s.recipe.name
    order by count(s) desc
    """)
List<MostBrewedRecipe> findMostBrewed(Long ownerId, Pageable pageable);
```

`BrewMethodRepository.findUsage` — owner filter goes in the join so shared methods still appear with a per-user (possibly zero) count:

```java
@Query(
    """
    select m.id as methodId, m.name as methodName, count(r) as recipeCount
    from BrewMethod m
    left join Recipe r on r.method = m and r.owner.id = :ownerId
    group by m.id, m.name
    order by count(r) desc, m.name asc
    """)
List<MethodUsage> findUsage(Long ownerId);
```

- [ ] **Step 4: Thread `ownerId` through the services**

- `CoffeeService.getMostUsed`: `recipeRepository.findMostUsedCoffees(currentOwnerId(), PageRequest.of(0, safeLimit))` (helper added in Task 1).
- `RecipeStatsService`: add a `CurrentUserProvider` field + `currentOwnerId()`; `getStats` uses `recipeRepository.existsByIdAndOwnerId(recipeId, ownerId)` then `findStatsByRecipeId(recipeId, ownerId)`; `getTopRated`/`getMostBrewed` pass `currentOwnerId()`.
- `BrewMethodService.getUsage`: add `CurrentUserProvider` field; `findUsage(currentUserProvider.require().getId())`.
- `DashboardService`: add `CurrentUserProvider` field; per-user counts:

```java
public DashboardSummaryResponse getSummary() {
  Long ownerId = currentUserProvider.require().getId();
  return new DashboardSummaryResponse(
      coffeeRepository.countByOwnerId(ownerId),
      brewMethodRepository.count(),
      recipeRepository.countByOwnerId(ownerId),
      recipeRepository.countByFavoriteTrueAndOwnerId(ownerId),
      brewSessionRepository.countByOwnerId(ownerId),
      brewSessionRepository.findAverageRating(ownerId));
}
```

(`brewMethodRepository.count()` stays global — methods are shared.)

- [ ] **Step 5: Update the service tests**

In `CoffeeServiceTest.getMostUsed*`, `RecipeStatsServiceTest`, `BrewMethodServiceTest.getUsage*`, `DashboardServiceTest`: stub `currentUserProvider.require()` → a `User` with an id, and update the `verify`/`when` to the new `ownerId`-carrying signatures. Assert the owner id is passed, e.g.:

```java
verify(brewSessionRepository).findTopRated(eq(42L), any(Pageable.class));
```

- [ ] **Step 6: Run the analytics tests**

Run: `sh mvnw -Dtest=CoffeeServiceTest,RecipeStatsServiceTest,BrewMethodServiceTest,DashboardServiceTest,BrewSessionRepositoryTest test`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java src/test/java
git commit -m "feat(api): scope analytics and dashboard to the current owner"
```

---

### Task 5: Owner-scope AI recipe improvement

**Files:**
- Modify: `src/main/java/com/brewdeck/brewdeck_api/ai/RecipeImprovementService.java`
- Test: `src/test/java/com/brewdeck/brewdeck_api/ai/RecipeImprovementServiceTest.java`

**Interfaces:**
- Consumes: `RecipeRepository.findByIdAndOwnerId` (Task 2), `CurrentUserProvider`.

- [ ] **Step 1: Add failing not-owned test**

In `RecipeImprovementServiceTest.java`:

```java
@Test
void improve_shouldThrowNotFound_whenRecipeOwnedByAnotherUser() {
  when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
  when(recipeRepository.findByIdAndOwnerId(99L, 42L)).thenReturn(Optional.empty());

  assertThatThrownBy(() -> recipeImprovementService.improve(99L))
      .isInstanceOf(EntityNotFoundException.class);
}
```

Add a `@Mock CurrentUserProvider currentUserProvider;` field to the test and update existing tests that stub `recipeRepository.findById` to stub `findByIdAndOwnerId` and `currentUserProvider.require()`.

- [ ] **Step 2: Run it, verify it fails**

Run: `sh mvnw -Dtest=RecipeImprovementServiceTest test`
Expected: FAIL — service still calls `findById`.

- [ ] **Step 3: Owner-scope the recipe lookup**

In `RecipeImprovementService.java`, add a `private final CurrentUserProvider currentUserProvider;` field and change the recipe lookup:

```java
Recipe recipe =
    recipeRepository
        .findByIdAndOwnerId(recipeId, currentUserProvider.require().getId())
        .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));
```

(The session sub-query below already reads sessions of this owned recipe, so it needs no change.)

- [ ] **Step 4: Run it, verify it passes**

Run: `sh mvnw -Dtest=RecipeImprovementServiceTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/brewdeck/brewdeck_api/ai src/test/java/com/brewdeck/brewdeck_api/ai
git commit -m "feat(api): owner-scope AI recipe improvement lookup"
```

---

### Task 6: Integration ownership seeding + cross-user isolation test

**Files:**
- Modify: `src/test/java/com/brewdeck/brewdeck_api/common/PostgresIntegrationTest.java` (helper accessor)
- Modify: analytics/dashboard/workflow integration tests that seed rows via `repository.save(...)`: `TopRatedRecipesIntegrationTest`, `MostBrewedRecipesIntegrationTest`, `MethodUsageIntegrationTest`, `MostUsedCoffeesIntegrationTest`, `DashboardSummaryIntegrationTest`, `RecipeStatsIntegrationTest`, `BrewingWorkflowIntegrationTest` (and any other that persists coffees/recipes/sessions directly)
- Create: `src/test/java/com/brewdeck/brewdeck_api/integration/CrossUserIsolationIntegrationTest.java`

**Interfaces:**
- Produces: `PostgresIntegrationTest.mockUser()` returning the seeded `User`.
- Consumes: the owner-scoped reads from Tasks 1–4.

- [ ] **Step 1: Expose the seeded user from the base class**

In `PostgresIntegrationTest.java` add:

```java
protected User mockUser() {
  return userRepository
      .findByEmail(MOCK_USER_EMAIL)
      .orElseThrow(() -> new IllegalStateException("mock user not seeded"));
}
```

- [ ] **Step 2: Stamp the seeded owner on directly-persisted rows**

In each integration test that persists a coffee/recipe/session via a repository (not via the API), set `owner(mockUser())` on the builder (e.g. `Coffee.builder()....owner(mockUser()).build()`, likewise recipes and sessions). Rows created through `mockMvc.perform(post(...))` already get the owner stamped by the service — leave those.

- [ ] **Step 3: Run the touched integration tests, verify they pass under owner scoping**

Run: `sh mvnw -Dtest=TopRatedRecipesIntegrationTest,MostBrewedRecipesIntegrationTest,MethodUsageIntegrationTest,MostUsedCoffeesIntegrationTest,DashboardSummaryIntegrationTest,RecipeStatsIntegrationTest,BrewingWorkflowIntegrationTest test`
Expected: PASS (rankings/counts now see the seeded owner's rows).

- [ ] **Step 4: Add the cross-user isolation test**

Create `CrossUserIsolationIntegrationTest.java`: persist a second user and a coffee/recipe/session owned by them; as the default `@WithMockUser` user, assert:
- `GET /api/coffees` (`$.content`) does not contain the foreign coffee.
- `GET /api/coffees/{foreignId}` returns 404.
- `GET /api/recipes/top-rated` does not include the foreign recipe.
- `GET /api/dashboard/summary` counts exclude the foreign rows.

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class CrossUserIsolationIntegrationTest extends PostgresIntegrationTest {
  // @Autowired MockMvc, repositories; persist a "other@brewdeck.test" User + owned rows,
  // then assert the four expectations above with jsonPath.
}
```

- [ ] **Step 5: Run it**

Run: `sh mvnw -Dtest=CrossUserIsolationIntegrationTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/test/java/com/brewdeck/brewdeck_api
git commit -m "test(api): seed owners in integration tests and cover cross-user isolation"
```

---

### Task 7: Enforce `owner_id NOT NULL` (V7)

**Files:**
- Create: `src/main/resources/db/migration/V7__owner_not_null.sql`
- Modify: `src/main/java/com/brewdeck/brewdeck_api/coffee/Coffee.java`
- Modify: `src/main/java/com/brewdeck/brewdeck_api/recipe/Recipe.java`
- Modify: `src/main/java/com/brewdeck/brewdeck_api/session/BrewSession.java`

**Interfaces:**
- Consumes: every persisted test row now sets an owner (Tasks 1–6) — precondition for the constraint.

- [ ] **Step 1: Write the migration**

`V7__owner_not_null.sql`:

```sql
-- Slice B.2: owner is now a hard invariant. Every row was backfilled in V6 and
-- every create stamps an owner, so tightening to NOT NULL is safe.
ALTER TABLE coffees       ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE recipes       ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE brew_sessions ALTER COLUMN owner_id SET NOT NULL;
```

- [ ] **Step 2: Make the entity associations non-null**

In `Coffee.java`, `Recipe.java`, `BrewSession.java` change the owner join column:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "owner_id", nullable = false)
private User owner;
```

- [ ] **Step 3: Run the full integration suite (Flyway applies V7)**

Run: `sh mvnw -Dtest="*IntegrationTest" test`
Expected: PASS — no `NOT NULL` violations, confirming all seeded rows now have owners.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/migration/V7__owner_not_null.sql src/main/java/com/brewdeck/brewdeck_api/coffee/Coffee.java src/main/java/com/brewdeck/brewdeck_api/recipe/Recipe.java src/main/java/com/brewdeck/brewdeck_api/session/BrewSession.java
git commit -m "feat(api): enforce owner_id NOT NULL on owned resources"
```

---

### Task 8: Full verification + docs

**Files:**
- Modify: `.claude/project-state.md`
- Modify: `.claude/roadmap.md`
- Modify: `docs/product/roadmap.md`
- Modify: `docs/architecture/database-design.md`

- [ ] **Step 1: Format + full verify + PMD**

```bash
sh mvnw spotless:apply
sh mvnw clean verify
sh mvnw pmd:check
```
Expected: BUILD SUCCESS on all three. Fix anything that fails before proceeding.

- [ ] **Step 2: Update the state/roadmap docs**

- `.claude/project-state.md`: move Slice B.2 to done in "Recently Worked On"; set "Current Phase" to note B.2 complete; set Immediate Next Step to Slice C (account UX).
- `.claude/roadmap.md` and `docs/product/roadmap.md`: mark Slice B (and B.2) Done.
- `docs/architecture/database-design.md`: V7 row in the migrations table; change the owner_id note to "NOT NULL, owner-scoped reads"; update the three table descriptions from "nullable" to "required".

- [ ] **Step 3: Commit**

```bash
git add .claude docs
git commit -m "docs: record per-user read isolation (Slice B.2)"
```

- [ ] **Step 4: Push + open PR**

```bash
git push -u origin feature/auth-ownership-reads
gh pr create --base develop --title "feat(api): per-user read isolation (Slice B.2)" --body "<summary + verification>"
```

---

## Self-Review

**Spec coverage:** lists (Tasks 1–3 specs), by-id/update/delete/favorite (1–3), stats+analytics+dashboard per-user (4), AI improve (5), 404 cross-user (1–3,5,6), public-share/brew-methods left global (untouched by design), NOT NULL V7 + non-null entities (7), integration seeding + isolation test (6), docs (8). All spec sections mapped.

**Placeholder scan:** Task 6 Step 4 and Task 8 Step 4 intentionally describe test/PR-body content to author in place (concrete assertions enumerated); all code steps show real code. No TBD/TODO.

**Type consistency:** `currentOwnerId()` helper name used consistently; `findByIdAndOwnerId`/`existsByIdAndOwnerId`/`countByOwnerId`/`findByFavoriteTrueAndOwnerId`/`countByFavoriteTrueAndOwnerId`/`findByCoffeeIdAndOwnerId`/`findByMethodIdAndOwnerId`/`findMostUsedCoffees(ownerId,…)`/`findAverageRating(ownerId)`/`findStatsByRecipeId(recipeId,ownerId)`/`findTopRated(ownerId,…)`/`findMostBrewed(ownerId,…)`/`findUsage(ownerId)` names match across producing and consuming tasks.
