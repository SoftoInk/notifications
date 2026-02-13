package com.novacomp.notifications.event;

import com.novacomp.notifications.Notification;
import com.novacomp.notifications.NotificationListener;
import com.novacomp.notifications.NotificationResult;

/**
 * Abstraction for publishing notification outcomes to registered listeners.
 *
 * <p>Extracting this from {@link com.novacomp.notifications.NotificationService}
 * improves testability and allows alternative publishing strategies (e.g.
 * asynchronous, event-bus backed) to be swapped in without modifying the
 * service itself.</p>
 */
public interface NotificationEventPublisher {

    /**
     * Publishes a notification result to all registered listeners.
     *
     * @param notification the notification that was sent
     * @param result       the outcome of the send attempt
     */
    void publish(Notification notification, NotificationResult result);

    /**
     * Registers a listener that will be notified of every send outcome.
     *
     * @param listener the listener to add
     */
    void addListener(NotificationListener listener);
}
