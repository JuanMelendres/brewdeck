package com.brewdeck.brewdeck_api.auth.emailchange;

import jakarta.validation.constraints.NotBlank;

/** Body of the confirm and cancel calls: the raw token from the emailed link. */
public record EmailChangeTokenRequest(@NotBlank String token) {}
