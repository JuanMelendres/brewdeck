package com.brewdeck.brewdeck_api.auth;

import java.time.Instant;

public record AuthResponse(String token, Instant expiresAt, String email) {}
