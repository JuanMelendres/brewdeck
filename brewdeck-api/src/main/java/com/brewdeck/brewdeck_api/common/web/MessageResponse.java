package com.brewdeck.brewdeck_api.common.web;

/**
 * Simple response carrying a single human-readable message. Used by endpoints that acknowledge an
 * action without returning a resource (e.g. password-reset request, resend verification).
 */
public record MessageResponse(String message) {}
