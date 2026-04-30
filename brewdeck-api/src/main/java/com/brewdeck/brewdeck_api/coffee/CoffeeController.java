package com.brewdeck.brewdeck_api.coffee;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coffees")
@RequiredArgsConstructor
public class CoffeeController {

  private final CoffeeService coffeeService;

  @GetMapping
  public List<CoffeeResponse> findAll() {
    return coffeeService.findAll();
  }

  @GetMapping("/{id}")
  public CoffeeResponse findById(@PathVariable Long id) {
    return coffeeService.findById(id);
  }

  @PostMapping
  public CoffeeResponse create(@Valid @RequestBody CoffeeRequest request) {
    return coffeeService.create(request);
  }

  @PutMapping("/{id}")
  public CoffeeResponse update(@PathVariable Long id, @Valid @RequestBody CoffeeRequest request) {
    return coffeeService.update(id, request);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    coffeeService.delete(id);
  }
}
