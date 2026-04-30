package com.brewdeck.brewdeck_api.coffee;

import java.time.LocalDateTime;

public record CoffeeResponse(
    Long id,
    String name,
    String brand,
    String origin,
    String region,
    String farm,
    String producer,
    String variety,
    String process,
    String roastLevel,
    String notesPrimary,
    String notesSecondary,
    String acidity,
    String body,
    String sweetness,
    String bitterness,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  public static CoffeeResponse fromEntity(Coffee coffee) {
    return new CoffeeResponse(
        coffee.getId(),
        coffee.getName(),
        coffee.getBrand(),
        coffee.getOrigin(),
        coffee.getRegion(),
        coffee.getFarm(),
        coffee.getProducer(),
        coffee.getVariety(),
        coffee.getProcess(),
        coffee.getRoastLevel(),
        coffee.getNotesPrimary(),
        coffee.getNotesSecondary(),
        coffee.getAcidity(),
        coffee.getBody(),
        coffee.getSweetness(),
        coffee.getBitterness(),
        coffee.getDescription(),
        coffee.getCreatedAt(),
        coffee.getUpdatedAt());
  }
}
