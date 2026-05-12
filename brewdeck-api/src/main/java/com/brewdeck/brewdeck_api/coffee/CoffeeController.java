package com.brewdeck.brewdeck_api.coffee;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coffees")
@RequiredArgsConstructor
public class CoffeeController {

  private final CoffeeService coffeeService;

  @GetMapping
  public ResponseEntity<List<CoffeeResponse>> findAll() {
    return ResponseEntity.ok(coffeeService.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<CoffeeResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(coffeeService.findById(id));
  }

  @PostMapping
  public ResponseEntity<CoffeeResponse> create(@Valid @RequestBody CoffeeRequest request) {
    CoffeeResponse response = coffeeService.create(request);

    URI location = URI.create("/api/coffees/" + response.id());

    return ResponseEntity.created(location).body(response);
  }

  @PutMapping("/{id}")
  public ResponseEntity<CoffeeResponse> update(
      @PathVariable Long id, @Valid @RequestBody CoffeeRequest request) {
    return ResponseEntity.ok(coffeeService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    coffeeService.delete(id);

    return ResponseEntity.noContent().build();
  }
}
