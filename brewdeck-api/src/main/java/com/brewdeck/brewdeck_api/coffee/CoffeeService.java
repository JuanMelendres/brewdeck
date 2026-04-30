package com.brewdeck.brewdeck_api.coffee;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoffeeService {

  private final CoffeeRepository coffeeRepository;

  public List<CoffeeResponse> findAll() {
    return coffeeRepository.findAll().stream().map(CoffeeResponse::fromEntity).toList();
  }

  public CoffeeResponse findById(Long id) {
    Coffee coffee =
        coffeeRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Coffee not found"));

    return CoffeeResponse.fromEntity(coffee);
  }

  public CoffeeResponse create(CoffeeRequest request) {
    Coffee coffee =
        Coffee.builder()
            .name(request.name())
            .brand(request.brand())
            .origin(request.origin())
            .region(request.region())
            .farm(request.farm())
            .producer(request.producer())
            .variety(request.variety())
            .process(request.process())
            .roastLevel(request.roastLevel())
            .notesPrimary(request.notesPrimary())
            .notesSecondary(request.notesSecondary())
            .acidity(request.acidity())
            .body(request.body())
            .sweetness(request.sweetness())
            .bitterness(request.bitterness())
            .description(request.description())
            .build();

    return CoffeeResponse.fromEntity(coffeeRepository.save(coffee));
  }

  public CoffeeResponse update(Long id, CoffeeRequest request) {
    Coffee coffee =
        coffeeRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Coffee not found"));

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
    coffee.setAcidity(request.acidity());
    coffee.setBody(request.body());
    coffee.setSweetness(request.sweetness());
    coffee.setBitterness(request.bitterness());
    coffee.setDescription(request.description());

    return CoffeeResponse.fromEntity(coffeeRepository.save(coffee));
  }

  public void delete(Long id) {
    if (!coffeeRepository.existsById(id)) {
      throw new EntityNotFoundException("Coffee not found");
    }

    coffeeRepository.deleteById(id);
  }
}
