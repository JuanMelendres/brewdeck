package com.brewdeck.brewdeck_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagAdminService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Anonymous callers learn which UI languages are on, in both flag states (ADR-015). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicUiConfigIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private FeatureFlagAdminService featureFlagAdminService;

  private void setFlag(boolean enabled) {
    featureFlagAdminService.setEnabled(
        FeatureKeys.I18N_SPANISH, "test", enabled, "integration-test");
  }

  @AfterEach
  void turnTheFlagBackOff() {
    setFlag(false);
  }

  @Test
  void flagOff_onlyEnglish_withoutAuthentication() throws Exception {
    setFlag(false);

    mockMvc
        .perform(get("/api/public/ui-config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.languages.length()").value(1))
        .andExpect(jsonPath("$.languages[0]").value("en"));
  }

  @Test
  void flagOn_englishAndSpanish() throws Exception {
    setFlag(true);

    mockMvc
        .perform(get("/api/public/ui-config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.languages[0]").value("en"))
        .andExpect(jsonPath("$.languages[1]").value("es"));
  }
}
