package com.brewdeck.brewdeck_api.featureflag;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only endpoint the frontend calls once per session to learn which client-exposed features are
 * available to the current user. Evaluated with the authenticated user's context so rollout is
 * consistent per user. Returns only the allow-listed flags in {@link FrontendFeatureFlag}.
 */
@RestController
@RequestMapping("/api/feature-flags")
@RequiredArgsConstructor
@Tag(name = "Feature Flags", description = "Client-exposed feature availability")
public class FeatureFlagController {

  private final FeatureFlagService featureFlagService;
  private final CurrentUserProvider currentUserProvider;

  @GetMapping
  @Operation(
      summary = "Get feature flags for the current user",
      description =
          "Returns the availability of each frontend-exposed feature as a {alias: boolean} map."
              + " The backend remains the source of truth; hidden UI is not a security boundary.")
  public ResponseEntity<FeatureFlagsResponse> getFeatureFlags() {
    FeatureFlagContext context = FeatureFlagContext.ofUser(currentUserProvider.require().getId());

    Map<String, Boolean> features = new LinkedHashMap<>();
    for (FrontendFeatureFlag flag : FrontendFeatureFlag.values()) {
      features.put(flag.frontendAlias(), featureFlagService.isEnabled(flag.backendKey(), context));
    }
    return ResponseEntity.ok(new FeatureFlagsResponse(features));
  }
}
