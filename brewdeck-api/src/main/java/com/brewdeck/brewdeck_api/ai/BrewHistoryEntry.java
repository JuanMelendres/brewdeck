package com.brewdeck.brewdeck_api.ai;

public record BrewHistoryEntry(
    Integer rating,
    String actualGrind,
    Integer actualTemp,
    String actualTime,
    String tasteResult,
    String adjustmentNotes) {}
