package com.novacomp.notifications;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable bag of provider credentials and settings.
 *
 * <p>Create instances with the generated Lombok builder:</p>
 * <pre>{@code
 * ProviderConfig config = ProviderConfig.builder()
 *         .apiKey("SG.xxxx")
 *         .property("fromEmail", "noreply@example.com")
 *         .build();
 * }</pre>
 *
 * <p>Sensitive fields ({@code apiKey}, {@code apiSecret}) are excluded
 * from {@link #toString()} to prevent accidental logging of secrets.</p>
 */
@Getter
@Builder
@ToString(exclude = {"apiKey", "apiSecret"})
public class ProviderConfig {

    /** Primary API key or account identifier. */
    private final String apiKey;

    /** Secondary secret (auth token, secret key, etc.). */
    private final String apiSecret;

    /** Arbitrary provider-specific key/value settings. */
    @Singular
    private final Map<String, String> properties;

    /**
     * Returns an unmodifiable view of all provider-specific properties.
     * This is a defense-in-depth override ensuring immutability regardless
     * of the underlying Lombok-generated collection type.
     */
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Returns a property value, or {@code null} if not present.
     */
    public String getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Returns a property value, throwing if absent or blank.
     *
     * @throws IllegalArgumentException if the property is missing
     */
    public String getRequiredProperty(String key) {
        String value = properties.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required provider property missing: " + key);
        }
        return value;
    }
}
