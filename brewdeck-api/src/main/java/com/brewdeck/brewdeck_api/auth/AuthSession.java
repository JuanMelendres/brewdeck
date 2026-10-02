package com.brewdeck.brewdeck_api.auth;

/**
 * What the service hands the controller after authenticating: the public {@link AuthResponse} body,
 * and the raw refresh token that the controller puts only into the httpOnly cookie (ADR-013).
 */
public record AuthSession(AuthResponse response, String refreshToken) {}
