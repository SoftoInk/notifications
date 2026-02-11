package com.novacomp.notifications;

import com.novacomp.notifications.exception.NotificationException;

/**
 * Strategy interface for delivering a specific notification type through a provider.
 *
 * <p>Each provider (SendGrid, Twilio, Firebase, …) implements this interface
 * for the notification type it supports. The {@link NotificationService}
 * dispatches to the correct sender based on the runtime type of the notification.</p>
 *
 * <p><strong>Implementing a new provider</strong>: implement this interface,
 * bind it to the appropriate {@code Notification} subclass, and register
 * it with {@link NotificationService.Builder#registerSender}.</p>
 *
 * @param <T> the notification type this sender can deliver
 */
public interface NotificationSender<T extends Notification> {

    /**
     * Delivers the given notification through the provider.
     *
     * @param notification the notification to send
     * @return a result describing the outcome
     * @throws NotificationException on unrecoverable delivery errors
     */
    NotificationResult send(T notification) throws NotificationException;

    /**
     * Returns the concrete {@link Notification} subclass this sender handles.
     * Used by the service to build its dispatch table.
     */
    Class<T> getSupportedType();

    /**
     * Human-readable provider name (e.g. "SendGrid", "Twilio").
     */
    String getProviderName();
}
