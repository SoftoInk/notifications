package com.novacomp.notifications.channel.email;

import com.novacomp.notifications.NotificationResult;
import com.novacomp.notifications.NotificationSender;
import com.novacomp.notifications.ProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * Simulated <a href="https://documentation.mailgun.com/en/latest/api-sending-messages.html">
 * Mailgun Messages API</a> email sender.
 *
 * <h3>Required configuration</h3>
 * <ul>
 *   <li>{@code apiKey} — Mailgun API key</li>
 *   <li>{@code domain} (property) — sending domain (e.g. {@code mg.example.com})</li>
 *   <li>{@code fromEmail} (property) — default sender address</li>
 * </ul>
 */
public class MailgunEmailSender implements NotificationSender<EmailNotification> {

    private static final Logger log = LoggerFactory.getLogger(MailgunEmailSender.class);

    private final ProviderConfig config;

    public MailgunEmailSender(ProviderConfig config) {
        Objects.requireNonNull(config, "ProviderConfig must not be null");
        Objects.requireNonNull(config.getApiKey(), "Mailgun API key is required");
        this.config = config;
    }

    @Override
    public NotificationResult send(EmailNotification email) {
        String domain = config.getRequiredProperty("domain");
        String from = email.getFrom() != null ? email.getFrom()
                : config.getRequiredProperty("fromEmail");

        /*
         * Simulate: POST https://api.mailgun.net/v3/{domain}/messages
         * Auth:     Basic api:{apiKey}
         * Form data: from, to, cc, bcc, subject, text, html, attachment
         * Response:  200 OK
         * {
         *   "id": "<message-id@domain>",
         *   "message": "Queued. Thank you."
         * }
         */

        log.info("[Mailgun] POST /v3/{}/messages — from={}, to={}, subject=\"{}\"",
                domain, from, email.getRecipient(), email.getSubject());

        String messageId = "<" + UUID.randomUUID().toString().substring(0, 20)
                + "@" + domain + ">";

        log.info("[Mailgun] 200 OK — id={}, message=\"Queued. Thank you.\"", messageId);

        return NotificationResult.queued(email.getId(), getProviderName(), messageId);
    }

    @Override
    public Class<EmailNotification> getSupportedType() {
        return EmailNotification.class;
    }

    @Override
    public String getProviderName() {
        return "Mailgun";
    }
}
