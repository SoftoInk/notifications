package com.novacomp.notifications.exception;

import lombok.Getter;

/**
 * Thrown when a provider accepts the request but delivery fails
 * (network error, rate-limit, invalid credentials, etc.).
 *
 * <p>Carries the {@link #providerName} and, when available, the
 * HTTP {@link #statusCode} returned by the upstream API.</p>
 */
@Getter
public class DeliveryException extends NotificationException {

    private final String providerName;
    private final int statusCode;

    public DeliveryException(String providerName, String message) {
        this(providerName, message, 0);
    }

    public DeliveryException(String providerName, String message, int statusCode) {
        super(String.format("[%s] %s", providerName, message));
        this.providerName = providerName;
        this.statusCode = statusCode;
    }

    public DeliveryException(String providerName, String message, Throwable cause) {
        super(String.format("[%s] %s", providerName, message), cause);
        this.providerName = providerName;
        this.statusCode = 0;
    }
}
