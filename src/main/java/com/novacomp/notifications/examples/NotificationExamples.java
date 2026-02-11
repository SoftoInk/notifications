package com.novacomp.notifications.examples;

import com.novacomp.notifications.*;
import com.novacomp.notifications.channel.email.Attachment;
import com.novacomp.notifications.channel.email.EmailNotification;
import com.novacomp.notifications.channel.email.SendGridEmailSender;
import com.novacomp.notifications.channel.push.FirebasePushSender;
import com.novacomp.notifications.channel.push.PushNotification;
import com.novacomp.notifications.channel.sms.SmsNotification;
import com.novacomp.notifications.channel.sms.TwilioSmsSender;
import com.novacomp.notifications.exception.NotificationException;
import com.novacomp.notifications.exception.ValidationException;
import com.novacomp.notifications.retry.RetryPolicy;
import com.novacomp.notifications.template.MessageTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Runnable examples demonstrating the Novacomp Notifications library.
 */
public class NotificationExamples {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Novacomp Notifications Library — Examples ===\n");

        basicUsage();
        templateExample();
        asyncExample();
        errorHandlingExample();
        batchSendExample();

        System.out.println("\n=== All examples completed ===");
    }

    // ---------------------------------------------------------------
    // 1. Basic usage: configure service, send one of each channel
    // ---------------------------------------------------------------
    static void basicUsage() {
        System.out.println("--- 1. Basic Usage ---");

        // Configure providers
        ProviderConfig emailConfig = ProviderConfig.builder()
                .apiKey("SG.fake-sendgrid-key")
                .property("fromEmail", "noreply@myapp.com")
                .property("fromName", "MyApp")
                .build();

        ProviderConfig smsConfig = ProviderConfig.builder()
                .apiKey("AC_fake_twilio_sid")
                .apiSecret("fake_twilio_auth_token")
                .property("fromNumber", "+15551234567")
                .build();

        ProviderConfig pushConfig = ProviderConfig.builder()
                .apiKey("fake-firebase-server-key")
                .property("projectId", "myapp-12345")
                .build();

        // Build the service
        try (NotificationService service = NotificationService.builder()
                .registerSender(new SendGridEmailSender(emailConfig))
                .registerSender(new TwilioSmsSender(smsConfig))
                .registerSender(new FirebasePushSender(pushConfig))
                .retryPolicy(RetryPolicy.fixed(3, Duration.ofSeconds(1)))
                .addListener((notification, result) ->
                        System.out.printf("  [Listener] %s → %s (provider: %s)%n",
                                notification.getChannelType(), result.getStatus(), result.getProviderName()))
                .build()) {

            // Send an email
            NotificationResult emailResult = service.send(
                    EmailNotification.builder()
                            .recipient("alice@example.com")
                            .subject("Welcome to MyApp!")
                            .message("Thanks for signing up, Alice.")
                            .htmlBody("<h1>Welcome!</h1><p>Thanks for signing up, Alice.</p>")
                            .cc("onboarding@myapp.com")
                            .attachment(Attachment.builder()
                                    .filename("guide.txt")
                                    .contentType("text/plain")
                                    .content("Getting started guide…".getBytes(StandardCharsets.UTF_8))
                                    .build())
                            .build());
            System.out.println("  Email result: " + emailResult);

            // Send an SMS
            NotificationResult smsResult = service.send(
                    SmsNotification.builder()
                            .recipient("+50688881234")
                            .message("Your verification code is 483920")
                            .build());
            System.out.println("  SMS result: " + smsResult);

            // Send a push notification
            NotificationResult pushResult = service.send(
                    PushNotification.builder()
                            .recipient("dJx8kL3mN7pQ2rS5tU9vW0xY1zA4bC6dE8fG0hI")
                            .title("New Order")
                            .message("Your order #ORD-12345 has been confirmed!")
                            .datum("orderId", "ORD-12345")
                            .datum("screen", "order_detail")
                            .priority(PushNotification.Priority.HIGH)
                            .build());
            System.out.println("  Push result: " + pushResult);
        }

        System.out.println();
    }

    // ---------------------------------------------------------------
    // 2. Message templates
    // ---------------------------------------------------------------
    static void templateExample() {
        System.out.println("--- 2. Templates ---");

        MessageTemplate welcomeTemplate = MessageTemplate.of(
                "Hello {{name}}, welcome to {{app}}! Your account #{{id}} is ready.");

        String message = welcomeTemplate.render(Map.of(
                "name", "Bob",
                "app", "MyApp",
                "id", "42"));

        System.out.println("  Rendered: " + message);
        System.out.println();
    }

    // ---------------------------------------------------------------
    // 3. Async sending
    // ---------------------------------------------------------------
    static void asyncExample() throws Exception {
        System.out.println("--- 3. Async Sending ---");

        ProviderConfig emailConfig = ProviderConfig.builder()
                .apiKey("SG.fake-key")
                .property("fromEmail", "noreply@myapp.com")
                .build();

        try (NotificationService service = NotificationService.builder()
                .registerSender(new SendGridEmailSender(emailConfig))
                .build()) {

            CompletableFuture<NotificationResult> future = service.sendAsync(
                    EmailNotification.builder()
                            .recipient("async@example.com")
                            .subject("Async test")
                            .message("This was sent asynchronously!")
                            .build());

            System.out.println("  Send initiated, doing other work…");

            NotificationResult result = future.get();
            System.out.println("  Async result: " + result);
        }

        System.out.println();
    }

    // ---------------------------------------------------------------
    // 4. Error handling
    // ---------------------------------------------------------------
    static void errorHandlingExample() {
        System.out.println("--- 4. Error Handling ---");

        ProviderConfig emailConfig = ProviderConfig.builder()
                .apiKey("SG.fake-key")
                .property("fromEmail", "noreply@myapp.com")
                .build();

        try (NotificationService service = NotificationService.builder()
                .registerSender(new SendGridEmailSender(emailConfig))
                .build()) {

            // Validation error: invalid email
            service.send(EmailNotification.builder()
                    .recipient("not-an-email")
                    .subject("Test")
                    .message("This will fail validation")
                    .build());

        } catch (ValidationException e) {
            System.out.printf("  Caught ValidationException — field: %s, message: %s%n",
                    e.getField(), e.getMessage());
        } catch (NotificationException e) {
            System.out.println("  Caught NotificationException: " + e.getMessage());
        }

        System.out.println();
    }

    // ---------------------------------------------------------------
    // 5. Batch sending
    // ---------------------------------------------------------------
    static void batchSendExample() {
        System.out.println("--- 5. Batch Sending ---");

        ProviderConfig smsConfig = ProviderConfig.builder()
                .apiKey("AC_fake_sid")
                .apiSecret("fake_token")
                .property("fromNumber", "+15559990000")
                .build();

        try (NotificationService service = NotificationService.builder()
                .registerSender(new TwilioSmsSender(smsConfig))
                .build()) {

            List<SmsNotification> batch = List.of(
                    SmsNotification.builder()
                            .recipient("+50688881111")
                            .message("Batch message 1")
                            .build(),
                    SmsNotification.builder()
                            .recipient("+50688882222")
                            .message("Batch message 2")
                            .build(),
                    SmsNotification.builder()
                            .recipient("+50688883333")
                            .message("Batch message 3")
                            .build()
            );

            List<NotificationResult> results = service.sendAll(batch);
            results.forEach(r -> System.out.println("  " + r.getStatus() + " → " + r.getProviderMessageId()));
        }

        System.out.println();
    }
}
