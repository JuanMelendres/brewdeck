package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.featureflag.FeatureDisabledException;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagAdminService;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagCache;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagRepository;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// Default @WithMockUser principal ("user") matches the seeded MOCK_USER_EMAIL.
@WithMockUser
class FeatureFlagIntegrationTest extends PostgresIntegrationTest {

  private static final String TEST_ENV = "test";

  @Autowired private MockMvc mockMvc;
  @Autowired private FeatureFlagService featureFlagService;
  @Autowired private FeatureFlagAdminService adminService;
  @Autowired private FeatureFlagCache cache;
  @MockitoSpyBean private FeatureFlagRepository repository;

  @AfterEach
  void restoreSeededFlag() {
    // The AI flag is seeded disabled in the test environment; restore that and clear the cache so
    // tests are order-independent no matter what each one toggled.
    adminService.setEnabled(FeatureKeys.AI_RECIPE_ASSISTANT, TEST_ENV, false, "test-teardown");
    cache.evictAll();
  }

  @Test
  void featureFlagsEndpoint_reportsAiDisabledInTestEnvironment() throws Exception {
    mockMvc
        .perform(get("/api/feature-flags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features.aiRecipeAssistant").value(false));
  }

  @Test
  void aiSuggest_returns404_withFeatureDisabledMessage_whenFlagOff() throws Exception {
    // Flag disabled in test env -> the gate returns a generic 404 before any data load or provider
    // call. The generic message keeps the disabled feature non-discoverable.
    mockMvc
        .perform(
            post("/api/recipes/suggest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coffeeId\":1,\"methodId\":2}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("This feature is not available"));
  }

  @Test
  void enablingFlag_flipsGateAndFrontendPayload() throws Exception {
    // Baseline: seeded disabled -> the gate blocks.
    assertThat(featureFlagService.isEnabled(FeatureKeys.AI_RECIPE_ASSISTANT)).isFalse();
    assertThatThrownBy(() -> featureFlagService.requireEnabled(FeatureKeys.AI_RECIPE_ASSISTANT))
        .isInstanceOf(FeatureDisabledException.class);

    adminService.setEnabled(FeatureKeys.AI_RECIPE_ASSISTANT, TEST_ENV, true, "tester");

    // With the flag on, the gate no longer blocks the protected operation (proving the flag — not
    // the frontend — is the release boundary), and the frontend payload reflects it. Asserted at
    // the gate/endpoint level so the test does not depend on shared-DB coffee/method rows.
    assertThat(featureFlagService.isEnabled(FeatureKeys.AI_RECIPE_ASSISTANT)).isTrue();
    assertThatCode(() -> featureFlagService.requireEnabled(FeatureKeys.AI_RECIPE_ASSISTANT))
        .doesNotThrowAnyException();

    mockMvc
        .perform(get("/api/feature-flags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features.aiRecipeAssistant").value(true));
  }

  @Test
  void repeatedReads_hitDatabaseOnce_thenAdminUpdateEvictsCache() {
    cache.evictAll();
    clearInvocations(repository);

    boolean first = featureFlagService.isEnabled(FeatureKeys.AI_RECIPE_ASSISTANT);
    boolean cached = featureFlagService.isEnabled(FeatureKeys.AI_RECIPE_ASSISTANT);

    assertThat(first).isFalse();
    assertThat(cached).isFalse();
    // Two reads, one datastore lookup: the second was served from the Caffeine cache.
    verify(repository, times(1))
        .findByFeatureKeyAndEnvironment(FeatureKeys.AI_RECIPE_ASSISTANT, TEST_ENV);

    // Admin update evicts the entry (it also reads internally, so reset the counter afterwards).
    adminService.setEnabled(FeatureKeys.AI_RECIPE_ASSISTANT, TEST_ENV, true, "tester");
    clearInvocations(repository);

    boolean afterUpdate = featureFlagService.isEnabled(FeatureKeys.AI_RECIPE_ASSISTANT);

    // Cache was evicted, so this read goes back to the database and sees the new value.
    assertThat(afterUpdate).isTrue();
    verify(repository, times(1))
        .findByFeatureKeyAndEnvironment(FeatureKeys.AI_RECIPE_ASSISTANT, TEST_ENV);
  }
}
