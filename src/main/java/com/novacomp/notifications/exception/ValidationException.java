package com.novacomp.notifications.exception;

import lombok.Getter;

/**
 * Thrown when a notification fails pre-send validation
 * (missing required fields, invalid format, etc.).
 *
 * <p>Carries the offending {@link #field} name so callers can surface
 * precise error feedback.</p>
 */
@Getter
public class ValidationException extends NotificationException {

    /** The field that failed validation. */
    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
}
