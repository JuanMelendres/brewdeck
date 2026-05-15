package com.brewdeck.brewdeck_api.common.error;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> validationErrors) {}
