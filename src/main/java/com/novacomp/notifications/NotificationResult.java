package com.novacomp.notifications;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Immutable value object describing the outcome of a notification send attempt.
 *
 * <p>Use {@link #isSuccessful()} for a quick check, or inspect
 * {@link #getStatus()} for finer-grained control.</p>
 */
@Getter
@Builder
@ToString
public class NotificationResult {

    public enum Status {
        /** The provider accepted and delivered the message. */
        SENT,
        /** The provider accepted the message; delivery is pending. */
        QUEUED,
        /** The send attempt failed. */
        FAILED
    }

    /** ID of the originating {@link Notification}. */
    private final String notificationId;

    /** Outcome of the attempt. */
    private final Status status;

    /** Provider-assigned message identifier (e.g. SendGrid message-id, Twilio SID). */
    private final String providerMessageId;

    /** Name of the provider that processed the request. */
    private final String providerName;

    /** When this result was created. */
    @Builder.Default
    private final Instant timestamp = Instant.now();

    /** Human-readable error description; {@code null} on success. */
    private final String errorMessage;

    /**
     * Convenience check — returns {@code true} when status is {@link Status#SENT}
     * or {@link Status#QUEUED}.
     */
    public boolean isSuccessful() {
        return status == Status.SENT || status == Status.QUEUED;
    }

    /* ---------- static factories for common cases ---------- */

    public static NotificationResult success(String notificationId,
                                             String providerName,
                                             String providerMessageId) {
        return NotificationResult.builder()
                .notificationId(notificationId)
                .status(Status.SENT)
                .providerName(providerName)
                .providerMessageId(providerMessageId)
                .build();
    }

    public static NotificationResult queued(String notificationId,
                                            String providerName,
                                            String providerMessageId) {
        return NotificationResult.builder()
                .notificationId(notificationId)
                .status(Status.QUEUED)
                .providerName(providerName)
                .providerMessageId(providerMessageId)
                .build();
    }

    public static NotificationResult failure(String notificationId,
                                             String providerName,
                                             String errorMessage) {
        return NotificationResult.builder()
                .notificationId(notificationId)
                .status(Status.FAILED)
                .providerName(providerName)
                .errorMessage(errorMessage)
                .build();
    }
}
