package com.novacomp.notifications.channel.email;

import com.novacomp.notifications.Notification;
import com.novacomp.notifications.exception.ValidationException;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Notification to be delivered via e-mail.
 *
 * <pre>{@code
 * EmailNotification email = EmailNotification.builder()
 *         .recipient("user@example.com")
 *         .from("noreply@myapp.com")
 *         .subject("Welcome!")
 *         .message("Thanks for signing up.")       // plain-text body
 *         .htmlBody("<h1>Welcome!</h1>")            // optional rich body
 *         .cc("manager@example.com")
 *         .build();
 * }</pre>
 */
@Getter
@SuperBuilder
@ToString(callSuper = true)
public class EmailNotification extends Notification {

    private static final String EMAIL_REGEX = "^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";

    /** Sender address (can also be set as a default in {@link com.novacomp.notifications.ProviderConfig}). */
    private final String from;

    /** Email subject line. */
    private final String subject;

    /** Optional HTML body. When present, providers send a multipart message. */
    private final String htmlBody;

    /** Carbon-copy recipients. */
    @Singular("cc")
    private final List<String> cc;

    /** Blind carbon-copy recipients. */
    @Singular("bcc")
    private final List<String> bcc;

    /** File attachments. */
    @Singular
    private final List<Attachment> attachments;

    /** Reply-to address. */
    private final String replyTo;

    @Override
    public void validate() throws ValidationException {
        requireNonBlank("recipient", getRecipient(), "Email recipient is required");
        requireValidEmail("recipient", getRecipient());

        requireNonBlank("subject", subject, "Email subject is required");

        if ((getMessage() == null || getMessage().isBlank())
                && (htmlBody == null || htmlBody.isBlank())) {
            throw new ValidationException("message",
                    "Either a plain-text message or an HTML body is required");
        }

        if (from != null && !from.isBlank()) {
            requireValidEmail("from", from);
        }
        if (replyTo != null && !replyTo.isBlank()) {
            requireValidEmail("replyTo", replyTo);
        }
        for (String addr : cc) {
            requireValidEmail("cc", addr);
        }
        for (String addr : bcc) {
            requireValidEmail("bcc", addr);
        }
    }

    @Override
    public String getChannelType() {
        return "EMAIL";
    }

    /* ---------- helpers ---------- */

    private static void requireNonBlank(String field, String value, String msg) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field, msg);
        }
    }

    private static void requireValidEmail(String field, String email) {
        if (email == null || !email.matches(EMAIL_REGEX)) {
            throw new ValidationException(field, "Invalid email format: " + email);
        }
    }
}
