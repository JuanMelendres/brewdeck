package com.brewdeck.brewdeck_api.auth.refresh;

/**
 * Legacy body for refresh/logout. The refresh token now normally arrives in the httpOnly cookie
 * (ADR-013), so the body and its field are optional during the transition.
 */
public record RefreshRequest(String refreshToken) {}
