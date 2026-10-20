package com.brewdeck.brewdeck_api.legal;

/**
 * Whether the privacy notice and terms are published, and their current version. Public, so the
 * sign-up page can read it before anyone logs in.
 */
public record LegalStatusResponse(String termsVersion, boolean published) {}
