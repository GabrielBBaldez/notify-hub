package io.notifyhub.queue.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for NotifyHub Kafka integration.
 *
 * <pre>
 * notify.queue.kafka.enabled=true
 * notify.queue.kafka.topic=notifyhub-notifications
 * notify.queue.kafka.consumer.enabled=true
 * notify.queue.kafka.consumer.group-id=notifyhub-group
 * notify.queue.kafka.consumer.concurrency=1
 * </pre>
 */
@ConfigurationProperties(prefix = "notify.queue.kafka")
public class KafkaQueueProperties {

    private boolean enabled = false;
    private String topic = "notifyhub-notifications";
    private Consumer consumer = new Consumer();

    public static class Consumer {
        private boolean enabled = true;
        private String groupId = "notifyhub-group";
        private int concurrency = 1;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }
        public int getConcurrency() { return concurrency; }
        public void setConcurrency(int concurrency) { this.concurrency = concurrency; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public Consumer getConsumer() { return consumer; }
    public void setConsumer(Consumer consumer) { this.consumer = consumer; }
}
