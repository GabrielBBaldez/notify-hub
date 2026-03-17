package io.notifyhub.core.event;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record NotificationEvent(
        String id,
        EventType type,
        String channelName,
        String recipient,
        String templateName,
        Duration latency,
        String errorMessage,
        Instant timestamp,
        Map<String, String> metadata
) {

    public NotificationEvent {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(channelName, "channelName must not be null");
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (metadata == null) {
            metadata = Map.of();
        } else {
            metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
        }
    }

    public static Builder builder(EventType type, String channelName) {
        return new Builder(type, channelName);
    }

    public static final class Builder {
        private String id;
        private final EventType type;
        private final String channelName;
        private String recipient;
        private String templateName;
        private Duration latency;
        private String errorMessage;
        private Instant timestamp;
        private Map<String, String> metadata;

        private Builder(EventType type, String channelName) {
            this.type = type;
            this.channelName = channelName;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder recipient(String recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder templateName(String templateName) {
            this.templateName = templateName;
            return this;
        }

        public Builder latency(Duration latency) {
            this.latency = latency;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public NotificationEvent build() {
            return new NotificationEvent(
                    id, type, channelName, recipient, templateName,
                    latency, errorMessage, timestamp, metadata
            );
        }
    }
}
