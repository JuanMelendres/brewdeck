package com.brewdeck.brewdeck_api.legal;

import com.brewdeck.brewdeck_api.featureflag.FeatureFlagService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/legal")
@RequiredArgsConstructor
@Tag(name = "Legal", description = "Privacy notice and terms status")
public class LegalController {

  private final FeatureFlagService featureFlagService;

  @GetMapping
  @Operation(
      summary = "Current terms version and whether the legal texts are published",
      description =
          "Public. The web shows the legal pages and the sign-up consent only when published.")
  public LegalStatusResponse status() {
    return new LegalStatusResponse(
        LegalTerms.CURRENT_VERSION, featureFlagService.isEnabled(FeatureKeys.LEGAL_TERMS));
  }
}
