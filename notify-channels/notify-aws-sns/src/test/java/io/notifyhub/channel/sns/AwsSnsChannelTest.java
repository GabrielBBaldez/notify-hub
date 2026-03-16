package io.notifyhub.channel.sns;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsSnsChannel")
class AwsSnsChannelTest {

    @Mock
    private SnsClient snsClient;

    private AwsSnsConfig config;
    private AwsSnsConfig configWithTopic;

    @BeforeEach
    void setUp() {
        config = AwsSnsConfig.builder()
                .region("us-east-1")
                .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .build();

        configWithTopic = AwsSnsConfig.builder()
                .region("us-east-1")
                .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .topicArn("arn:aws:sns:us-east-1:123456789012:MyTopic")
                .build();
    }

    @Test
    @DisplayName("Should return 'aws-sns' as channel name")
    void shouldReturnChannelName() {
        AwsSnsChannel channel = new AwsSnsChannel(config, snsClient);
        assertEquals("aws-sns", channel.getName());
    }

    @Test
    @DisplayName("Should be available when credentials are configured")
    void shouldBeAvailableWhenCredentialsConfigured() {
        AwsSnsChannel channel = new AwsSnsChannel(config, snsClient);
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("Should publish to topicArn when recipient is not an ARN or phone number")
    void shouldPublishToTopicArn() {
        AwsSnsChannel channel = new AwsSnsChannel(configWithTopic, snsClient);

        PublishResponse response = PublishResponse.builder().messageId("msg-123").build();
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(response);

        Notification notification = new Notification(
                "my-alias", "aws-sns", null, null, "Hello from SNS", Map.of());

        assertDoesNotThrow(() -> channel.send(notification));

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());

        PublishRequest req = captor.getValue();
        assertEquals("arn:aws:sns:us-east-1:123456789012:MyTopic", req.topicArn());
        assertEquals("Hello from SNS", req.message());
    }

    @Test
    @DisplayName("Should publish to targetArn when recipient starts with 'arn:'")
    void shouldPublishToTargetArnWhenRecipientIsArn() {
        AwsSnsChannel channel = new AwsSnsChannel(config, snsClient);

        PublishResponse response = PublishResponse.builder().messageId("msg-456").build();
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(response);

        String targetArn = "arn:aws:sns:us-east-1:123456789012:endpoint/GCM/MyApp/abc123";
        Notification notification = new Notification(
                targetArn, "aws-sns", null, null, "Push notification", Map.of());

        assertDoesNotThrow(() -> channel.send(notification));

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());

        PublishRequest req = captor.getValue();
        assertEquals(targetArn, req.targetArn());
        assertEquals("Push notification", req.message());
    }

    @Test
    @DisplayName("Should publish to phone number when recipient starts with '+'")
    void shouldPublishToPhoneNumberWhenRecipientStartsWithPlus() {
        AwsSnsChannel channel = new AwsSnsChannel(config, snsClient);

        PublishResponse response = PublishResponse.builder().messageId("msg-789").build();
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(response);

        Notification notification = new Notification(
                "+15555555555", "aws-sns", null, null, "SMS via SNS", Map.of());

        assertDoesNotThrow(() -> channel.send(notification));

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());

        PublishRequest req = captor.getValue();
        assertEquals("+15555555555", req.phoneNumber());
        assertEquals("SMS via SNS", req.message());
    }

    @Test
    @DisplayName("Should include subject when provided in params")
    void shouldIncludeSubjectFromParams() {
        AwsSnsChannel channel = new AwsSnsChannel(configWithTopic, snsClient);

        PublishResponse response = PublishResponse.builder().messageId("msg-sub").build();
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(response);

        Notification notification = new Notification(
                "alias", "aws-sns", null, null, "Message body",
                Map.of("subject", "My Subject"));

        assertDoesNotThrow(() -> channel.send(notification));

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());

        assertEquals("My Subject", captor.getValue().subject());
    }

    @Test
    @DisplayName("Should throw NotificationSendException when no target can be resolved")
    void shouldThrowWhenNoTargetCanBeResolved() {
        AwsSnsChannel channel = new AwsSnsChannel(config, snsClient);

        Notification notification = new Notification(
                "not-an-arn-or-phone", "aws-sns", null, null, "Message", Map.of());

        NotificationSendException ex = assertThrows(NotificationSendException.class,
                () -> channel.send(notification));

        assertTrue(ex.getMessage().contains("Cannot resolve SNS target"));
    }
}
