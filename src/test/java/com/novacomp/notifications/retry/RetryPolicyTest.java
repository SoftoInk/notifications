package com.novacomp.notifications.retry;

import com.novacomp.notifications.NotificationResult;
import com.novacomp.notifications.exception.DeliveryException;
import com.novacomp.notifications.exception.NotificationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RetryPolicy")
class RetryPolicyTest {

    private static NotificationResult ok() {
        return NotificationResult.success("id", "Test", "msg-ok");
    }

    private static NotificationResult fail() {
        return NotificationResult.failure("id", "Test", "temporary error");
    }

    // ---------------------------------------------------------------
    // No retry
    // ---------------------------------------------------------------

    @Test
    @DisplayName("none() executes exactly once")
    void noneExecutesOnce() {
        AtomicInteger calls = new AtomicInteger();
        RetryPolicy policy = RetryPolicy.none();

        NotificationResult result = policy.execute(() -> {
            calls.incrementAndGet();
            return ok();
        });

        assertThat(result.isSuccessful()).isTrue();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("none() does not retry on failure result")
    void noneDoesNotRetryFailedResult() {
        AtomicInteger calls = new AtomicInteger();
        RetryPolicy policy = RetryPolicy.none();

        NotificationResult result = policy.execute(() -> {
            calls.incrementAndGet();
            return fail();
        });

        assertThat(result.isSuccessful()).isFalse();
        assertThat(calls.get()).isEqualTo(1);
    }

    // ---------------------------------------------------------------
    // Fixed retry
    // ---------------------------------------------------------------

    @Test
    @DisplayName("fixed() succeeds immediately when first attempt passes")
    void fixedSucceedsImmediately() {
        AtomicInteger calls = new AtomicInteger();
        RetryPolicy policy = RetryPolicy.fixed(3, Duration.ofMillis(1));

        NotificationResult result = policy.execute(() -> {
            calls.incrementAndGet();
            return ok();
        });

        assertThat(result.isSuccessful()).isTrue();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("fixed() retries up to maxAttempts on failure results")
    void fixedRetriesOnFailureResult() {
        AtomicInteger calls = new AtomicInteger();
        RetryPolicy policy = RetryPolicy.fixed(3, Duration.ofMillis(1));

        NotificationResult result = policy.execute(() -> {
            calls.incrementAndGet();
            return fail();
        });

        assertThat(result.isSuccessful()).isFalse();
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("fixed() retries and eventually succeeds")
    void fixedRetriesAndSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        RetryPolicy policy = RetryPolicy.fixed(3, Duration.ofMillis(1));

        NotificationResult result = policy.execute(() -> {
            if (calls.incrementAndGet() < 3) return fail();
            return ok();
        });

        assertThat(result.isSuccessful()).isTrue();
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("fixed() retries on exception and re-throws after max attempts")
    void fixedRetriesOnException() {
        AtomicInteger calls = new AtomicInteger();
        RetryPolicy policy = RetryPolicy.fixed(2, Duration.ofMillis(1));

        assertThatThrownBy(() -> policy.execute(() -> {
            calls.incrementAndGet();
            throw new DeliveryException("Test", "network error");
        }))
                .isInstanceOf(DeliveryException.class)
                .hasMessageContaining("network error");

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("fixed() recovers from exception on later attempt")
    void fixedRecoversFromException() {
        AtomicInteger calls = new AtomicInteger();
        RetryPolicy policy = RetryPolicy.fixed(3, Duration.ofMillis(1));

        NotificationResult result = policy.execute(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new DeliveryException("Test", "transient error");
            }
            return ok();
        });

        assertThat(result.isSuccessful()).isTrue();
        assertThat(calls.get()).isEqualTo(2);
    }

    // ---------------------------------------------------------------
    // Exponential backoff
    // ---------------------------------------------------------------

    @Test
    @DisplayName("exponentialBackoff() doubles the delay each attempt")
    void exponentialBackoffDelay() {
        RetryPolicy policy = RetryPolicy.exponentialBackoff(4, Duration.ofMillis(100));

        assertThat(policy.calculateDelay(1)).isEqualTo(Duration.ofMillis(100));
        assertThat(policy.calculateDelay(2)).isEqualTo(Duration.ofMillis(200));
        assertThat(policy.calculateDelay(3)).isEqualTo(Duration.ofMillis(400));
    }

    // ---------------------------------------------------------------
    // Edge cases
    // ---------------------------------------------------------------

    @Test
    @DisplayName("maxAttempts=1 means no retries (same as none)")
    void singleAttemptMeansNoRetry() {
        AtomicInteger calls = new AtomicInteger();
        RetryPolicy policy = RetryPolicy.fixed(1, Duration.ofMillis(1));

        NotificationResult result = policy.execute(() -> {
            calls.incrementAndGet();
            return fail();
        });

        assertThat(calls.get()).isEqualTo(1);
        assertThat(result.isSuccessful()).isFalse();
    }

    @Test
    @DisplayName("constructor rejects maxAttempts < 1")
    void rejectsInvalidMaxAttempts() {
        assertThatThrownBy(() -> RetryPolicy.fixed(0, Duration.ofMillis(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
