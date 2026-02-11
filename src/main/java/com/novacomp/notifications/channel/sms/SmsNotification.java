package com.novacomp.notifications.channel.sms;

import com.novacomp.notifications.Notification;
import com.novacomp.notifications.exception.ValidationException;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Notification to be delivered as an SMS text message.
 *
 * <pre>{@code
 * SmsNotification sms = SmsNotification.builder()
 *         .recipient("+50688881234")
 *         .message("Your verification code is 483920")
 *         .from("+15551234567")
 *         .build();
 * }</pre>
 */
@Getter
@SuperBuilder
@ToString(callSuper = true)
public class SmsNotification extends Notification {

    /** E.164 phone number pattern: optional '+', country code, 7–15 digits. */
    private static final String PHONE_REGEX = "^\\+?[1-9]\\d{6,14}$";

    /** Maximum SMS segment length (Twilio concatenated limit). */
    private static final int MAX_MESSAGE_LENGTH = 1600;

    /** Sender phone number (can also default from provider config). */
    private final String from;

    @Override
    public void validate() throws ValidationException {
        if (getRecipient() == null || getRecipient().isBlank()) {
            throw new ValidationException("recipient", "Phone number is required");
        }
        if (!getRecipient().matches(PHONE_REGEX)) {
            throw new ValidationException("recipient",
                    "Invalid phone number (expected E.164 format): " + getRecipient());
        }
        if (getMessage() == null || getMessage().isBlank()) {
            throw new ValidationException("message", "SMS message body is required");
        }
        if (getMessage().length() > MAX_MESSAGE_LENGTH) {
            throw new ValidationException("message",
                    "SMS message exceeds " + MAX_MESSAGE_LENGTH + " character limit ("
                            + getMessage().length() + " chars)");
        }
    }

    @Override
    public String getChannelType() {
        return "SMS";
    }
}
