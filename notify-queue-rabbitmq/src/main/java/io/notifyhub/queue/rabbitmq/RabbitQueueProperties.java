package io.notifyhub.queue.rabbitmq;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for NotifyHub RabbitMQ integration.
 *
 * <pre>
 * notify.queue.rabbitmq.enabled=true
 * notify.queue.rabbitmq.queue-name=notifyhub-notifications
 * notify.queue.rabbitmq.exchange-name=notifyhub-exchange
 * notify.queue.rabbitmq.routing-key=notification
 * notify.queue.rabbitmq.consumer.enabled=true
 * notify.queue.rabbitmq.consumer.concurrency=1
 * notify.queue.rabbitmq.consumer.max-concurrency=5
 * </pre>
 */
@ConfigurationProperties(prefix = "notify.queue.rabbitmq")
public class RabbitQueueProperties {

    private boolean enabled = false;
    private String queueName = "notifyhub-notifications";
    private String exchangeName = "notifyhub-exchange";
    private String routingKey = "notification";
    private Consumer consumer = new Consumer();

    public static class Consumer {
        private boolean enabled = true;
        private int concurrency = 1;
        private int maxConcurrency = 5;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getConcurrency() { return concurrency; }
        public void setConcurrency(int concurrency) { this.concurrency = concurrency; }
        public int getMaxConcurrency() { return maxConcurrency; }
        public void setMaxConcurrency(int maxConcurrency) { this.maxConcurrency = maxConcurrency; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getQueueName() { return queueName; }
    public void setQueueName(String queueName) { this.queueName = queueName; }
    public String getExchangeName() { return exchangeName; }
    public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }
    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
    public Consumer getConsumer() { return consumer; }
    public void setConsumer(Consumer consumer) { this.consumer = consumer; }
}
