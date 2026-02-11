package com.novacomp.notifications.channel.push;

import com.novacomp.notifications.Notification;
import com.novacomp.notifications.exception.ValidationException;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * Notification to be delivered as a mobile push notification.
 *
 * <pre>{@code
 * PushNotification push = PushNotification.builder()
 *         .recipient("dJx8kL3...deviceToken")
 *         .title("New message")
 *         .message("You have a new order!")
 *         .datum("orderId", "ORD-12345")
 *         .priority(PushNotification.Priority.HIGH)
 *         .build();
 * }</pre>
 *
 * <p>When {@code recipient} is a device registration token the notification
 * is sent to that single device. When {@code topic} is set instead (or additionally),
 * providers that support topic-based messaging (e.g. FCM) will broadcast to all
 * subscribers of that topic.</p>
 */
@Getter
@SuperBuilder
@ToString(callSuper = true)
public class PushNotification extends Notification {

    public enum Priority { NORMAL, HIGH }

    /** Title displayed in the notification shade. */
    private final String title;

    /** Optional image URL for rich push. */
    private final String imageUrl;

    /** Arbitrary custom data payload forwarded to the client app. */
    @Singular("datum")
    private final Map<String, String> data;

    /** Topic for pub/sub broadcast (FCM topics, APNs channels). */
    private final String topic;

    /** Delivery priority hint. */
    @lombok.Builder.Default
    private final Priority priority = Priority.NORMAL;

    @Override
    public void validate() throws ValidationException {
        boolean hasToken = getRecipient() != null && !getRecipient().isBlank();
        boolean hasTopic = topic != null && !topic.isBlank();

        if (!hasToken && !hasTopic) {
            throw new ValidationException("recipient",
                    "Either a device token (recipient) or a topic is required");
        }
        if (title == null || title.isBlank()) {
            throw new ValidationException("title", "Push notification title is required");
        }
        if (getMessage() == null || getMessage().isBlank()) {
            throw new ValidationException("message", "Push notification body is required");
        }
    }

    @Override
    public String getChannelType() {
        return "PUSH";
    }
}
