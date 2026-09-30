package com.brewdeck.brewdeck_api.auth;

import java.time.Instant;

/**
 * Public body of register/login/refresh. The refresh token is deliberately absent: it only ever
 * travels in the httpOnly {@code brewdeck_refresh} cookie, so page scripts never see it (ADR-013).
 */
public record AuthResponse(String token, Instant expiresAt, String email) {}
