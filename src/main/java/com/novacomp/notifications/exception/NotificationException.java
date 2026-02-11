package com.novacomp.notifications.exception;

/**
 * Base unchecked exception for all notification-related errors.
 *
 * <p>Unchecked by design so that callers are free to catch when they want
 * without being forced by the compiler. Subtypes let callers distinguish
 * validation errors from delivery failures when needed.</p>
 */
public class NotificationException extends RuntimeException {

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
