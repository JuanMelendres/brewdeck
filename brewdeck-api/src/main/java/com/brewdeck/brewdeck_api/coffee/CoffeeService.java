package com.brewdeck.brewdeck_api.coffee;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.common.pagination.PageResponse;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoffeeService {

  private static final int MIN_LIMIT = 1;
  private static final int MAX_LIMIT = 20;

  private final CoffeeRepository coffeeRepository;
  private final RecipeRepository recipeRepository;
  private final CurrentUserProvider currentUserProvider;

  public List<MostUsedCoffeeResponse> getMostUsed(int limit) {
    int safeLimit = Math.min(Math.max(limit, MIN_LIMIT), MAX_LIMIT);

    return recipeRepository
        .findMostUsedCoffees(currentOwnerId(), PageRequest.of(0, safeLimit))
        .stream()
        .map(
            row ->
                new MostUsedCoffeeResponse(
                    row.getCoffeeId(), row.getCoffeeName(), row.getRecipeCount()))
        .toList();
  }

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

  public CoffeeResponse findById(Long id) {
    Coffee coffee =
        coffeeRepository
            .findByIdAndOwnerId(id, currentOwnerId())
            .orElseThrow(() -> new EntityNotFoundException("Coffee not found"));

    return CoffeeResponse.fromEntity(coffee);
  }

  public CoffeeResponse create(CoffeeRequest request) {
    Coffee coffee = new Coffee();
    coffee.setOwner(currentUserProvider.require());
    applyRequest(coffee, request);

    Coffee saved = coffeeRepository.save(coffee);
    log.info("Created coffee id={}", saved.getId());

    return CoffeeResponse.fromEntity(saved);
  }

  public CoffeeResponse update(Long id, CoffeeRequest request) {
    Coffee coffee =
        coffeeRepository
            .findByIdAndOwnerId(id, currentOwnerId())
            .orElseThrow(() -> new EntityNotFoundException("Coffee not found"));

    applyRequest(coffee, request);

    Coffee saved = coffeeRepository.save(coffee);
    log.info("Updated coffee id={}", saved.getId());

    return CoffeeResponse.fromEntity(saved);
  }

  public void delete(Long id) {
    if (!coffeeRepository.existsByIdAndOwnerId(id, currentOwnerId())) {
      throw new EntityNotFoundException("Coffee not found");
    }

    coffeeRepository.deleteById(id);
    log.info("Deleted coffee id={}", id);
  }

  private Long currentOwnerId() {
    return currentUserProvider.require().getId();
  }

  private void applyRequest(Coffee coffee, CoffeeRequest request) {
    coffee.setName(request.name());
    coffee.setBrand(request.brand());
    coffee.setOrigin(request.origin());
    coffee.setRegion(request.region());
    coffee.setFarm(request.farm());
    coffee.setProducer(request.producer());
    coffee.setVariety(request.variety());
    coffee.setProcess(request.process());
    coffee.setRoastLevel(request.roastLevel());
    coffee.setNotesPrimary(request.notesPrimary());
    coffee.setNotesSecondary(request.notesSecondary());
    coffee.setAcidityScore(request.acidityScore());
    coffee.setBodyScore(request.bodyScore());
    coffee.setSweetnessScore(request.sweetnessScore());
    coffee.setBitternessScore(request.bitternessScore());
    coffee.setDescription(request.description());
  }
}
