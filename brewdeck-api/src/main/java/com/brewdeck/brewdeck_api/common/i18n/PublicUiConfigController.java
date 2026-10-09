package com.brewdeck.brewdeck_api.common.i18n;

import com.brewdeck.brewdeck_api.featureflag.FeatureFlagService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets the web server render pages in the right language before a user is known (ADR-015,
 * docs/architecture/i18n-spanish-rollout-tdd.md). Exposes only the UI languages, never other flags.
 */
@RestController
@RequestMapping("/api/public/ui-config")
@RequiredArgsConstructor
@Tag(name = "Public UI Config", description = "User-independent UI settings")
public class PublicUiConfigController {

  private final FeatureFlagService featureFlagService;

  @GetMapping
  @Operation(
      summary = "Get the UI languages users can choose",
      description =
          "Always includes \"en\"; includes \"es\" while the web-i18n-spanish flag is on."
              + " No authentication required.")
  public ResponseEntity<UiConfigResponse> getUiConfig() {
    List<String> languages =
        featureFlagService.isEnabled(FeatureKeys.I18N_SPANISH)
            ? List.of("en", "es")
            : List.of("en");
    return ResponseEntity.ok(new UiConfigResponse(languages));
  }
}
