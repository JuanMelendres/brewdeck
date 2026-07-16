package com.brewdeck.brewdeck_api.featureflag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class DatabaseFeatureFlagServiceTest {

  private static final String KEY = "brew-recipe-ai-assistant";
  private static final String ENV = "test";

  @Mock private FeatureFlagCache cache;
  @Mock private EnvironmentResolver environmentResolver;
  @Mock private RolloutEvaluator rolloutEvaluator;

  private DatabaseFeatureFlagService service;

  @BeforeEach
  void setUp() {
    service = new DatabaseFeatureFlagService(cache, environmentResolver, rolloutEvaluator);
    lenient().when(environmentResolver.resolve()).thenReturn(ENV);
  }

  @Test
  void isEnabled_true_whenFlagEnabledAndInRollout() {
    when(cache.get(KEY, ENV))
        .thenReturn(new FlagSnapshot(true, true, 100, FlagType.RELEASE, false));
    when(rolloutEvaluator.isIncluded(KEY, 100, null)).thenReturn(true);

    FeatureFlagEvaluation evaluation = service.evaluate(KEY, FeatureFlagContext.empty());

    assertThat(evaluation.enabled()).isTrue();
    assertThat(evaluation.reason()).isEqualTo(EvaluationReason.ENABLED);
    assertThat(evaluation.environment()).isEqualTo(ENV);
  }

  @Test
  void isEnabled_false_whenFlagDisabled() {
    when(cache.get(KEY, ENV))
        .thenReturn(new FlagSnapshot(true, false, 100, FlagType.RELEASE, false));

    FeatureFlagEvaluation evaluation = service.evaluate(KEY, FeatureFlagContext.empty());

    assertThat(evaluation.enabled()).isFalse();
    assertThat(evaluation.reason()).isEqualTo(EvaluationReason.DISABLED);
  }

  @Test
  void isEnabled_false_whenFlagMissing() {
    when(cache.get(KEY, ENV)).thenReturn(FlagSnapshot.absent());

    FeatureFlagEvaluation evaluation = service.evaluate(KEY, FeatureFlagContext.empty());

    assertThat(evaluation.enabled()).isFalse();
    assertThat(evaluation.reason()).isEqualTo(EvaluationReason.FLAG_NOT_FOUND);
  }

  @Test
  void isEnabled_false_whenDatastoreUnavailable() {
    when(cache.get(KEY, ENV)).thenReturn(FlagSnapshot.datastoreUnavailable());

    FeatureFlagEvaluation evaluation = service.evaluate(KEY, FeatureFlagContext.empty());

    assertThat(evaluation.enabled()).isFalse();
    assertThat(evaluation.reason()).isEqualTo(EvaluationReason.DATASTORE_UNAVAILABLE);
  }

  @Test
  void isEnabled_false_whenRolloutExcludesSubject() {
    when(cache.get(KEY, ENV)).thenReturn(new FlagSnapshot(true, true, 25, FlagType.RELEASE, false));
    when(rolloutEvaluator.isIncluded(KEY, 25, "user:7")).thenReturn(false);

    FeatureFlagEvaluation evaluation = service.evaluate(KEY, FeatureFlagContext.ofUser(7L));

    assertThat(evaluation.enabled()).isFalse();
    assertThat(evaluation.reason()).isEqualTo(EvaluationReason.ROLLOUT_EXCLUDED);
  }

  @Test
  void requireEnabled_passes_whenEnabled() {
    when(cache.get(KEY, ENV))
        .thenReturn(new FlagSnapshot(true, true, 100, FlagType.RELEASE, false));
    when(rolloutEvaluator.isIncluded(KEY, 100, null)).thenReturn(true);

    service.requireEnabled(KEY);
  }

  @Test
  void requireEnabled_throws404_whenReleaseFlagDisabled() {
    when(cache.get(KEY, ENV))
        .thenReturn(new FlagSnapshot(true, false, 100, FlagType.RELEASE, false));

    FeatureDisabledException ex =
        catchThrowableOfType(() -> service.requireEnabled(KEY), FeatureDisabledException.class);

    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ex.getFeatureKey()).isEqualTo(KEY);
  }

  @Test
  void requireEnabled_throws503_whenKillSwitchDisabled() {
    when(cache.get(KEY, ENV))
        .thenReturn(new FlagSnapshot(true, false, 100, FlagType.KILL_SWITCH, false));

    FeatureDisabledException ex =
        catchThrowableOfType(() -> service.requireEnabled(KEY), FeatureDisabledException.class);

    assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void requireEnabled_throws404_whenFlagMissing() {
    when(cache.get(KEY, ENV)).thenReturn(FlagSnapshot.absent());

    assertThatThrownBy(() -> service.requireEnabled(KEY))
        .isInstanceOf(FeatureDisabledException.class)
        .extracting(ex -> ((FeatureDisabledException) ex).getStatus())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }
}
