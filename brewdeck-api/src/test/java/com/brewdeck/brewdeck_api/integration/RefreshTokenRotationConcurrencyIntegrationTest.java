package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.refresh.InvalidRefreshTokenException;
import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenService;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Two requests rotating the same refresh token at once (AUTH-05.5). Before the conditional update
 * both could pass the "unused" check and each walk away with a fresh session; now exactly one
 * rotation succeeds and the other is treated as reuse.
 */
@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenRotationConcurrencyIntegrationTest extends PostgresIntegrationTest {

  private static final int CALLERS = 4;

  @Autowired private RefreshTokenService refreshTokenService;
  @Autowired private UserRepository userRepository;

  @RepeatedTest(5)
  void concurrentRotationsOfOneToken_onlyOneSucceeds() throws Exception {
    User user =
        userRepository.save(
            User.builder()
                .email("rotate-race-" + System.nanoTime() + "@example.com")
                .passwordHash("hash")
                .emailVerified(true)
                .createdAt(Instant.now())
                .build());
    String rawToken = refreshTokenService.issue(user);

    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(CALLERS);
    try {
      List<Future<Boolean>> results = new ArrayList<>();
      for (int i = 0; i < CALLERS; i++) {
        Callable<Boolean> rotate =
            () -> {
              start.await();
              try {
                refreshTokenService.rotate(rawToken);
                return true;
              } catch (InvalidRefreshTokenException e) {
                return false;
              }
            };
        results.add(pool.submit(rotate));
      }
      start.countDown();

      int succeeded = 0;
      for (Future<Boolean> result : results) {
        if (get(result)) {
          succeeded++;
        }
      }
      assertThat(succeeded).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  private static boolean get(Future<Boolean> result) throws InterruptedException {
    try {
      return result.get();
    } catch (ExecutionException e) {
      throw new AssertionError("rotation failed unexpectedly", e.getCause());
    }
  }
}
