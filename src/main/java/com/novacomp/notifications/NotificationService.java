package com.novacomp.notifications;

import com.novacomp.notifications.exception.NotificationException;
import com.novacomp.notifications.retry.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Central façade for sending notifications through any registered channel.
 *
 * <p>The service maintains a dispatch table that maps each {@link Notification}
 * subclass to its {@link NotificationSender}. When {@link #send} is called the
 * runtime type of the notification selects the appropriate sender, which is
 * invoked through the configured {@link RetryPolicy}. Registered
 * {@link NotificationListener}s are notified after each successful attempt.</p>
 *
 * <h3>Quick start</h3>
 * <pre>{@code
 * NotificationService service = NotificationService.builder()
 *         .registerSender(new SendGridEmailSender(emailConfig))
 *         .registerSender(new TwilioSmsSender(smsConfig))
 *         .registerSender(new FirebasePushSender(pushConfig))
 *         .retryPolicy(RetryPolicy.fixed(3, Duration.ofSeconds(1)))
 *         .addListener((n, r) -> log.info("{} → {}", n.getChannelType(), r.getStatus()))
 *         .build();
 *
 * NotificationResult result = service.send(
 *         EmailNotification.builder()
 *                 .recipient("user@example.com")
 *                 .subject("Welcome")
 *                 .message("Hello!")
 *                 .build());
 * }</pre>
 *
 * <p>Implements {@link AutoCloseable} so it can be used in try-with-resources;
 * closing shuts down the internal executor used for async sends.</p>
 */
public class NotificationService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final Map<Class<? extends Notification>, NotificationSender<?>> senders;
    private final List<NotificationListener> listeners;
    private final RetryPolicy retryPolicy;
    private final ExecutorService executorService;

    private NotificationService(Builder builder) {
        this.senders = Collections.unmodifiableMap(new HashMap<>(builder.senders));
        this.listeners = Collections.unmodifiableList(new ArrayList<>(builder.listeners));
        this.retryPolicy = builder.retryPolicy != null ? builder.retryPolicy : RetryPolicy.none();
        this.executorService = builder.executorService != null
                ? builder.executorService
                : Executors.newCachedThreadPool();
    }

    /* ------------------------------------------------------------------ */
    /*  Synchronous API                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * Validates and sends a notification through its registered sender.
     *
     * @param notification the notification to send
     * @param <T>          notification type
     * @return the send result
     * @throws com.novacomp.notifications.exception.ValidationException if validation fails
     * @throws NotificationException if no sender is registered or delivery fails
     */
    @SuppressWarnings("unchecked")
    public <T extends Notification> NotificationResult send(T notification) {
        Objects.requireNonNull(notification, "Notification must not be null");

        notification.validate();

        NotificationSender<T> sender = (NotificationSender<T>) senders.get(notification.getClass());
        if (sender == null) {
            throw new NotificationException(
                    "No sender registered for " + notification.getClass().getSimpleName()
                            + ". Register one via NotificationService.builder().registerSender(…)");
        }

        log.debug("Sending {} via {}", notification.getChannelType(), sender.getProviderName());

        NotificationResult result = retryPolicy.execute(() -> sender.send(notification));

        notifyListeners(notification, result);
        return result;
    }

    /**
     * Sends multiple notifications sequentially, collecting all results.
     * A failure in one notification does <em>not</em> stop the remaining ones.
     */
    public List<NotificationResult> sendAll(List<? extends Notification> notifications) {
        Objects.requireNonNull(notifications, "Notification list must not be null");

        List<NotificationResult> results = new ArrayList<>(notifications.size());
        for (Notification notification : notifications) {
            try {
                results.add(send(notification));
            } catch (NotificationException e) {
                log.error("Failed to send notification {}: {}", notification.getId(), e.getMessage());
                results.add(NotificationResult.failure(notification.getId(), "unknown", e.getMessage()));
            }
        }
        return results;
    }

    /* ------------------------------------------------------------------ */
    /*  Asynchronous API                                                   */
    /* ------------------------------------------------------------------ */

    /**
     * Sends a notification asynchronously.
     *
     * @return a future that completes with the result
     */
    public <T extends Notification> CompletableFuture<NotificationResult> sendAsync(T notification) {
        return CompletableFuture.supplyAsync(() -> send(notification), executorService);
    }

    /**
     * Sends all notifications asynchronously in parallel, returning a future
     * that completes when every notification has been processed.
     */
    public CompletableFuture<List<NotificationResult>> sendAllAsync(
            List<? extends Notification> notifications) {
        Objects.requireNonNull(notifications, "Notification list must not be null");

        List<CompletableFuture<NotificationResult>> futures = notifications.stream()
                .map(this::sendAsync)
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    /* ------------------------------------------------------------------ */
    /*  Internal                                                           */
    /* ------------------------------------------------------------------ */

    private void notifyListeners(Notification notification, NotificationResult result) {
        for (NotificationListener listener : listeners) {
            try {
                listener.onResult(notification, result);
            } catch (Exception e) {
                log.warn("NotificationListener threw an exception — ignoring", e);
            }
        }
    }

    @Override
    public void close() {
        executorService.shutdown();
    }

    /* ------------------------------------------------------------------ */
    /*  Builder                                                            */
    /* ------------------------------------------------------------------ */

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for assembling a {@link NotificationService}.
     */
    public static final class Builder {

        private final Map<Class<? extends Notification>, NotificationSender<?>> senders = new LinkedHashMap<>();
        private final List<NotificationListener> listeners = new ArrayList<>();
        private RetryPolicy retryPolicy;
        private ExecutorService executorService;

        private Builder() {}

        /**
         * Registers a sender for its declared notification type.
         * If a sender was already registered for the same type it is replaced.
         */
        public <T extends Notification> Builder registerSender(NotificationSender<T> sender) {
            Objects.requireNonNull(sender, "Sender must not be null");
            senders.put(sender.getSupportedType(), sender);
            return this;
        }

        /** Adds a listener that observes every send outcome. */
        public Builder addListener(NotificationListener listener) {
            Objects.requireNonNull(listener, "Listener must not be null");
            listeners.add(listener);
            return this;
        }

        /** Sets the retry policy applied to every send. Defaults to no retries. */
        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        /**
         * Provides a custom executor for async sends.
         * If not set, a cached thread pool is created internally.
         */
        public Builder executorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        /**
         * Builds the service. At least one sender must be registered.
         *
         * @throws IllegalStateException if no senders were registered
         */
        public NotificationService build() {
            if (senders.isEmpty()) {
                throw new IllegalStateException("At least one NotificationSender must be registered");
            }
            return new NotificationService(this);
        }
    }
}
