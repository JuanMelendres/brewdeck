package com.brewdeck.brewdeck_api.auth;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(@Size(max = 100) String displayName) {}
