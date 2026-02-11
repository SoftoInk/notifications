package com.novacomp.notifications;

import com.novacomp.notifications.exception.ValidationException;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all notification types.
 *
 * <p>Each channel (Email, SMS, Push) extends this class to add
 * channel-specific fields while sharing the common envelope:
 * recipient, message body, and arbitrary metadata.</p>
 *
 * <p>Subclasses must implement {@link #validate()} to enforce
 * their own field constraints, and {@link #getChannelType()} to
 * identify the channel for logging and routing.</p>
 *
 * <p>Instances are created through Lombok's {@code @SuperBuilder}
 * generated builders — e.g. {@code EmailNotification.builder()...build();}.</p>
 */
@Getter
@SuperBuilder
@ToString
public abstract class Notification {

    /** Unique identifier, auto-generated on creation. */
    @lombok.Builder.Default
    private final String id = UUID.randomUUID().toString();

    /** Timestamp of creation. */
    @lombok.Builder.Default
    private final Instant createdAt = Instant.now();

    /** The primary recipient address (email, phone number, device token, etc.). */
    private final String recipient;

    /** The plain-text message body. */
    private final String message;

    /** Free-form key/value metadata forwarded to the provider. */
    @Singular("metadatum")
    private final Map<String, String> metadata;

    /**
     * Validates that all required fields are present and correctly formatted.
     *
     * @throws ValidationException if any field fails validation
     */
    public abstract void validate() throws ValidationException;

    /**
     * Returns the channel identifier (e.g. "EMAIL", "SMS", "PUSH").
     */
    public abstract String getChannelType();
}
