package com.brewdeck.brewdeck_api.session;

import java.time.LocalDateTime;

public record BrewSessionResponse(
    Long id,
    Long recipeId,
    String recipeName,
    LocalDateTime brewedAt,
    String actualGrind,
    Integer actualTemp,
    String actualTime,
    String tasteResult,
    Integer rating,
    String adjustmentNotes) {

  public static BrewSessionResponse fromEntity(BrewSession session) {
    return new BrewSessionResponse(
        session.getId(),
        session.getRecipe().getId(),
        session.getRecipe().getName(),
        session.getBrewedAt(),
        session.getActualGrind(),
        session.getActualTemp(),
        session.getActualTime(),
        session.getTasteResult(),
        session.getRating(),
        session.getAdjustmentNotes());
  }
}
