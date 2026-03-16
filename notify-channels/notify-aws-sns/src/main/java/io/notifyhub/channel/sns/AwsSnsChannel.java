package io.notifyhub.channel.sns;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

/**
 * AWS SNS notification channel.
 *
 * <p>Supports publishing to:</p>
 * <ul>
 *   <li>A specific ARN (topic or platform endpoint) when the recipient starts with {@code "arn:"}</li>
 *   <li>The default topic ARN from config, when the recipient is not an ARN</li>
 *   <li>A phone number (SMS) when the recipient starts with {@code "+"}</li>
 * </ul>
 *
 * <pre>{@code
 * AwsSnsChannel sns = new AwsSnsChannel(
 *     AwsSnsConfig.builder()
 *         .region("us-east-1")
 *         .accessKeyId("AKIA...")
 *         .secretAccessKey("secret")
 *         .topicArn("arn:aws:sns:us-east-1:123456789012:MyTopic")
 *         .build()
 * );
 * }</pre>
 */
public class AwsSnsChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(AwsSnsChannel.class);

    private final AwsSnsConfig config;
    private final SnsClient snsClient;

    public AwsSnsChannel(AwsSnsConfig config) {
        this.config = config;
        this.snsClient = SnsClient.builder()
                .region(Region.of(config.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.getAccessKeyId(), config.getSecretAccessKey())))
                .build();
    }

    /** Package-private constructor for testing with a mock SnsClient. */
    AwsSnsChannel(AwsSnsConfig config, SnsClient snsClient) {
        this.config = config;
        this.snsClient = snsClient;
    }

    @Override
    public String getName() {
        return "aws-sns";
    }

    @Override
    public boolean isAvailable() {
        return config.getRegion() != null && !config.getRegion().isBlank()
                && config.getAccessKeyId() != null && !config.getAccessKeyId().isBlank()
                && config.getSecretAccessKey() != null && !config.getSecretAccessKey().isBlank();
    }

    @Override
    public void send(Notification notification) throws NotificationSendException {
        String recipient = notification.getRecipient();
        String message = notification.getRenderedContent();
        String subject = resolveSubject(notification);

        try {
            PublishRequest.Builder requestBuilder = PublishRequest.builder()
                    .message(message);

            if (recipient != null && recipient.startsWith("arn:")) {
                // Publish to a specific ARN (topic ARN or target ARN)
                requestBuilder.targetArn(recipient);
            } else if (config.getTopicArn() != null && !config.getTopicArn().isBlank()) {
                // Publish to the configured default topic ARN
                requestBuilder.topicArn(config.getTopicArn());
            } else if (recipient != null && recipient.startsWith("+")) {
                // Publish as SMS to a phone number
                requestBuilder.phoneNumber(recipient);
            } else {
                throw new NotificationSendException("aws-sns",
                        "Cannot resolve SNS target: recipient '" + recipient
                                + "' is not an ARN or phone number, and no default topicArn is configured");
            }

            if (subject != null && !subject.isBlank()) {
                requestBuilder.subject(subject);
            }

            PublishResponse response = snsClient.publish(requestBuilder.build());
            log.debug("AWS SNS message sent: messageId={}", response.messageId());

        } catch (NotificationSendException e) {
            throw e;
        } catch (SnsException e) {
            throw new NotificationSendException("aws-sns",
                    "AWS SNS publish failed: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new NotificationSendException("aws-sns",
                    "Failed to send AWS SNS notification: " + e.getMessage(), e);
        }
    }

    private String resolveSubject(Notification notification) {
        Object subject = notification.getParams().get("subject");
        return subject != null ? subject.toString() : null;
    }
}
