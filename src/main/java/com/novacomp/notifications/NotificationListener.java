package com.novacomp.notifications;

/**
 * Observer that is notified after every send attempt made by the
 * {@link NotificationService}.
 *
 * <p>Register listeners through
 * {@link NotificationService.Builder#addListener} to hook into
 * auditing, metrics collection, or chained workflows.</p>
 */
@FunctionalInterface
public interface NotificationListener {

    /**
     * Called after a notification send attempt completes (success or failure).
     *
     * @param notification the notification that was sent
     * @param result       the outcome of the send attempt
     */
    void onResult(Notification notification, NotificationResult result);
}
