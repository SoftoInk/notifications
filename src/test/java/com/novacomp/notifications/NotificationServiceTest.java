package com.novacomp.notifications;

import com.novacomp.notifications.channel.email.EmailNotification;
import com.novacomp.notifications.channel.push.PushNotification;
import com.novacomp.notifications.channel.sms.SmsNotification;
import com.novacomp.notifications.exception.NotificationException;
import com.novacomp.notifications.exception.ValidationException;
import com.novacomp.notifications.retry.RetryPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService")
class NotificationServiceTest {

    @Mock NotificationSender<EmailNotification> emailSender;
    @Mock NotificationSender<SmsNotification> smsSender;

    NotificationService service;

    @BeforeEach
    void setUp() {
        lenient().when(emailSender.getSupportedType()).thenReturn(EmailNotification.class);
        lenient().when(smsSender.getSupportedType()).thenReturn(SmsNotification.class);
    }

    @AfterEach
    void tearDown() {
        if (service != null) service.close();
    }

    private NotificationService.Builder baseBuilder() {
        return NotificationService.builder()
                .registerSender(emailSender)
                .registerSender(smsSender);
    }

    private EmailNotification validEmail() {
        return EmailNotification.builder()
                .recipient("test@example.com")
                .subject("Hi")
                .message("Hello")
                .build();
    }

    private SmsNotification validSms() {
        return SmsNotification.builder()
                .recipient("+50688881234")
                .message("Test")
                .build();
    }

    // ---------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------

    @Test
    @DisplayName("build() fails when no senders are registered")
    void buildFailsWithoutSenders() {
        assertThatThrownBy(() -> NotificationService.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least one");
    }

    // ---------------------------------------------------------------
    // Synchronous send
    // ---------------------------------------------------------------

    @Test
    @DisplayName("send() dispatches to the correct sender based on notification type")
    void sendDispatchesCorrectly() {
        NotificationResult expected = NotificationResult.success("id", "MockEmail", "msg-1");
        when(emailSender.send(any())).thenReturn(expected);

        service = baseBuilder().build();
        NotificationResult result = service.send(validEmail());

        assertThat(result).isSameAs(expected);
        verify(emailSender).send(any(EmailNotification.class));
        verify(smsSender, never()).send(any());
    }

    @Test
    @DisplayName("send() throws when no sender is registered for the notification type")
    void sendThrowsForUnregisteredType() {
        service = baseBuilder().build();

        PushNotification push = PushNotification.builder()
                .recipient("device-token")
                .title("Test")
                .message("Body")
                .build();

        assertThatThrownBy(() -> service.send(push))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("No sender registered");
    }

    @Test
    @DisplayName("send() propagates validation errors")
    void sendPropagatesValidationError() {
        service = baseBuilder().build();

        EmailNotification invalid = EmailNotification.builder()
                .recipient("not-an-email")
                .subject("Test")
                .message("Body")
                .build();

        assertThatThrownBy(() -> service.send(invalid))
                .isInstanceOf(ValidationException.class)
                .extracting(e -> ((ValidationException) e).getField())
                .isEqualTo("recipient");

        verify(emailSender, never()).send(any());
    }

    @Test
    @DisplayName("send() rejects null notification")
    void sendRejectsNull() {
        service = baseBuilder().build();
        assertThatThrownBy(() -> service.send(null))
                .isInstanceOf(NullPointerException.class);
    }

    // ---------------------------------------------------------------
    // Listeners
    // ---------------------------------------------------------------

    @Test
    @DisplayName("listeners are notified after successful send")
    void listenerNotifiedOnSuccess() {
        NotificationResult expected = NotificationResult.success("id", "Mock", "msg-1");
        when(emailSender.send(any())).thenReturn(expected);

        AtomicReference<NotificationResult> captured = new AtomicReference<>();
        service = baseBuilder()
                .addListener((n, r) -> captured.set(r))
                .build();

        service.send(validEmail());

        assertThat(captured.get()).isSameAs(expected);
    }

    @Test
    @DisplayName("a failing listener does not break the send")
    void failingListenerDoesNotBreakSend() {
        NotificationResult expected = NotificationResult.success("id", "Mock", "msg-1");
        when(emailSender.send(any())).thenReturn(expected);

        AtomicReference<NotificationResult> secondCapture = new AtomicReference<>();
        service = baseBuilder()
                .addListener((n, r) -> { throw new RuntimeException("Something was wrong..."); })
                .addListener((n, r) -> secondCapture.set(r))
                .build();

        NotificationResult result = service.send(validEmail());

        assertThat(result).isSameAs(expected);
        assertThat(secondCapture.get()).isSameAs(expected);
    }

    // ---------------------------------------------------------------
    // Retry integration
    // ---------------------------------------------------------------

    @Test
    @DisplayName("retries are applied when result is non-successful")
    void retriesOnFailedResult() {
        AtomicInteger attempts = new AtomicInteger();
        when(emailSender.send(any())).thenAnswer(invocation -> {
            if (attempts.incrementAndGet() < 3) {
                return NotificationResult.failure("id", "Mock", "temporary error");
            }
            return NotificationResult.success("id", "Mock", "msg-ok");
        });

        service = baseBuilder()
                .retryPolicy(RetryPolicy.fixed(3, Duration.ofMillis(10)))
                .build();

        NotificationResult result = service.send(validEmail());

        assertThat(result.isSuccessful()).isTrue();
        assertThat(attempts.get()).isEqualTo(3);
    }

    // ---------------------------------------------------------------
    // sendAll
    // ---------------------------------------------------------------

    @Test
    @DisplayName("sendAll collects results and does not stop on individual failure")
    void sendAllCollectsResults() {
        when(emailSender.send(any())).thenReturn(
                NotificationResult.success("1", "Mock", "msg-1"));
        when(smsSender.send(any())).thenThrow(
                new NotificationException("simulated failure"));

        service = baseBuilder().build();

        List<NotificationResult> results = service.sendAll(List.of(validEmail(), validSms()));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).isSuccessful()).isTrue();
        assertThat(results.get(1).isSuccessful()).isFalse();
    }

    // ---------------------------------------------------------------
    // Async
    // ---------------------------------------------------------------

    @Test
    @DisplayName("sendAsync completes with the send result")
    void sendAsyncReturnsResult() throws Exception {
        NotificationResult expected = NotificationResult.success("id", "Mock", "msg-1");
        when(emailSender.send(any())).thenReturn(expected);

        service = baseBuilder().build();

        CompletableFuture<NotificationResult> future = service.sendAsync(validEmail());
        NotificationResult result = future.get();

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("sendAsync propagates exceptions through the future")
    void sendAsyncPropagatesException() {
        service = baseBuilder().build();

        EmailNotification invalid = EmailNotification.builder()
                .recipient("bad")
                .subject("Test")
                .message("Body")
                .build();

        CompletableFuture<NotificationResult> future = service.sendAsync(invalid);

        assertThatThrownBy(future::get)
                .hasCauseInstanceOf(ValidationException.class);
    }

    // ---------------------------------------------------------------
    // sendAllAsync
    // ---------------------------------------------------------------

    @Test
    @DisplayName("sendAllAsync sends all notifications in parallel")
    void sendAllAsyncSendsInParallel() throws Exception {
        when(emailSender.send(any())).thenReturn(
                NotificationResult.success("1", "Mock", "msg-1"));
        when(smsSender.send(any())).thenReturn(
                NotificationResult.success("2", "Mock", "msg-2"));

        service = baseBuilder().build();

        List<NotificationResult> results = service.sendAllAsync(
                List.of(validEmail(), validSms())).get();

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(NotificationResult::isSuccessful);
    }
}
