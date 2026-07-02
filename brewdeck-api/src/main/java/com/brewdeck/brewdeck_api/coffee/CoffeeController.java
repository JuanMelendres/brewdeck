package com.brewdeck.brewdeck_api.coffee;

import com.brewdeck.brewdeck_api.common.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coffees")
@RequiredArgsConstructor
@Tag(name = "Coffees", description = "Manage coffees")
public class CoffeeController {

  private final CoffeeService coffeeService;

  @GetMapping
  @Operation(summary = "List coffees", description = "Returns a paginated list of coffees.")
  public ResponseEntity<PageResponse<CoffeeResponse>> findAll(
      @ModelAttribute CoffeeFilter filter,
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.ok(coffeeService.search(filter, pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get coffee by id")
  public ResponseEntity<CoffeeResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(coffeeService.findById(id));
  }

  @PostMapping
  @Operation(summary = "Create coffee")
  public ResponseEntity<CoffeeResponse> create(@Valid @RequestBody CoffeeRequest request) {
    CoffeeResponse response = coffeeService.create(request);

    URI location = URI.create("/api/coffees/" + response.id());

    return ResponseEntity.created(location).body(response);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update coffee")
  public ResponseEntity<CoffeeResponse> update(
      @PathVariable Long id, @Valid @RequestBody CoffeeRequest request) {
    return ResponseEntity.ok(coffeeService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete coffee")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    coffeeService.delete(id);

    return ResponseEntity.noContent().build();
  }
}
