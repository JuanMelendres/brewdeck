package com.brewdeck.brewdeck_api.featureflag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RolloutEvaluatorTest {

  private final RolloutEvaluator evaluator = new RolloutEvaluator();

  @Test
  void fullRollout_includesEveryone_evenWithoutSubject() {
    assertThat(evaluator.isIncluded("flag", 100, null)).isTrue();
    assertThat(evaluator.isIncluded("flag", 100, "user:1")).isTrue();
  }

  @Test
  void zeroRollout_excludesEveryone() {
    assertThat(evaluator.isIncluded("flag", 0, "user:1")).isFalse();
    assertThat(evaluator.isIncluded("flag", 0, null)).isFalse();
  }

  @Test
  void partialRolloutWithoutSubject_excludes_toFailSafe() {
    assertThat(evaluator.isIncluded("flag", 50, null)).isFalse();
  }

  @Test
  void sameSubjectAndFlag_isDeterministic() {
    boolean first = evaluator.isIncluded("flag", 50, "user:123");
    boolean second = evaluator.isIncluded("flag", 50, "user:123");
    assertThat(second).isEqualTo(first);
  }

  @Test
  void partialRollout_partitionsSubjects_intoBothBuckets() {
    int included = 0;
    for (int i = 0; i < 200; i++) {
      if (evaluator.isIncluded("flag", 50, "user:" + i)) {
        included++;
      }
    }
    // A deterministic 50% split over 200 stable subjects must land some in and some out.
    assertThat(included).isGreaterThan(0).isLessThan(200);
  }

  @Test
  void boundaryPercentages_areHonoured() {
    // A subject in bucket b is included iff b < percentage; 1% and 99% must both be reachable.
    long includedAt1 = countIncluded(1);
    long includedAt99 = countIncluded(99);
    assertThat(includedAt1).isLessThan(includedAt99);
  }

  private long countIncluded(int percentage) {
    long count = 0;
    for (int i = 0; i < 500; i++) {
      if (evaluator.isIncluded("flag", percentage, "user:" + i)) {
        count++;
      }
    }
    return count;
  }
}
