package com.novacomp.notifications.retry;

import com.novacomp.notifications.NotificationResult;
import com.novacomp.notifications.exception.NotificationException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Configurable retry policy for notification delivery attempts.
 *
 * <p>Supports fixed-delay and exponential-backoff strategies.
 * Create instances through the static factories:</p>
 * <pre>{@code
 * RetryPolicy.none();                                       // no retries
 * RetryPolicy.fixed(3, Duration.ofSeconds(1));              // 3 attempts, 1 s between each
 * RetryPolicy.exponentialBackoff(4, Duration.ofMillis(500));// 4 attempts, 500 ms → 1 s → 2 s
 * }</pre>
 */
@Getter
public class RetryPolicy {

    private static final Logger log = LoggerFactory.getLogger(RetryPolicy.class);

    public enum Strategy { FIXED, EXPONENTIAL_BACKOFF }

    private final int maxAttempts;
    private final Duration initialDelay;
    private final Strategy strategy;
    private final Predicate<NotificationResult> retryCondition;

    private RetryPolicy(int maxAttempts,
                         Duration initialDelay,
                         Strategy strategy,
                         Predicate<NotificationResult> retryCondition) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
        this.strategy = strategy;
        this.retryCondition = retryCondition;
    }

    /* ---------- static factories ---------- */

    /** No retries — execute exactly once. */
    public static RetryPolicy none() {
        return new RetryPolicy(1, Duration.ZERO, Strategy.FIXED, result -> false);
    }

    /** Retry up to {@code maxAttempts} times with a constant delay between each. */
    public static RetryPolicy fixed(int maxAttempts, Duration delay) {
        return new RetryPolicy(maxAttempts, delay, Strategy.FIXED, r -> !r.isSuccessful());
    }

    /** Retry with exponential backoff (delay doubles after each attempt). */
    public static RetryPolicy exponentialBackoff(int maxAttempts, Duration initialDelay) {
        return new RetryPolicy(maxAttempts, initialDelay, Strategy.EXPONENTIAL_BACKOFF, r -> !r.isSuccessful());
    }

    /**
     * Executes the given action, retrying according to this policy.
     *
     * <p>Retries happen when the result matches the {@link #retryCondition}
     * or when the action throws a {@link NotificationException}.</p>
     *
     * @param action supplier that performs the send and returns a result
     * @return the final {@link NotificationResult}
     * @throws NotificationException if all attempts fail with an exception
     */
    public NotificationResult execute(Supplier<NotificationResult> action) {
        NotificationResult lastResult = null;
        NotificationException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                lastResult = action.get();
                lastException = null;

                if (!retryCondition.test(lastResult)) {
                    return lastResult;
                }
                log.debug("Attempt {}/{} returned non-successful result, retrying…",
                        attempt, maxAttempts);
            } catch (NotificationException e) {
                lastException = e;
                lastResult = null;
                log.debug("Attempt {}/{} threw {}: {}",
                        attempt, maxAttempts, e.getClass().getSimpleName(), e.getMessage());
            }

            if (attempt < maxAttempts) {
                sleep(calculateDelay(attempt));
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        return lastResult;
    }

    Duration calculateDelay(int attempt) {
        return switch (strategy) {
            case FIXED -> initialDelay;
            case EXPONENTIAL_BACKOFF -> initialDelay.multipliedBy((long) Math.pow(2, attempt - 1));
        };
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NotificationException("Retry sleep interrupted", e);
        }
    }
}
