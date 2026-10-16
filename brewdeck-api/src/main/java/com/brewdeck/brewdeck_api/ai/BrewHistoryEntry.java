package com.brewdeck.brewdeck_api.ai;

import java.math.BigDecimal;

public record BrewHistoryEntry(
    Integer rating,
    String actualGrind,
    BigDecimal actualTemp,
    String actualTime,
    String tasteResult,
    String adjustmentNotes) {}
