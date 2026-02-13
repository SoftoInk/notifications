package com.novacomp.notifications.event;

import com.novacomp.notifications.Notification;
import com.novacomp.notifications.NotificationListener;
import com.novacomp.notifications.NotificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Default synchronous implementation of {@link NotificationEventPublisher}.
 *
 * <p>Iterates through all registered listeners, catching and logging any
 * exception a listener throws so that one misbehaving listener cannot
 * prevent others from being notified.</p>
 */
public class DefaultNotificationEventPublisher implements NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationEventPublisher.class);

    private final List<NotificationListener> listeners;

    public DefaultNotificationEventPublisher() {
        this.listeners = new ArrayList<>();
    }

    public DefaultNotificationEventPublisher(List<NotificationListener> listeners) {
        Objects.requireNonNull(listeners, "Listeners list must not be null");
        this.listeners = new ArrayList<>(listeners);
    }

    @Override
    public void publish(Notification notification, NotificationResult result) {
        Objects.requireNonNull(notification, "Notification must not be null");
        Objects.requireNonNull(result, "NotificationResult must not be null");

        for (NotificationListener listener : listeners) {
            try {
                listener.onResult(notification, result);
            } catch (Exception e) {
                log.warn("NotificationListener threw an exception — ignoring", e);
            }
        }
    }

    @Override
    public void addListener(NotificationListener listener) {
        Objects.requireNonNull(listener, "Listener must not be null");
        listeners.add(listener);
    }

    /**
     * Returns an unmodifiable view of the registered listeners.
     */
    public List<NotificationListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }
}
