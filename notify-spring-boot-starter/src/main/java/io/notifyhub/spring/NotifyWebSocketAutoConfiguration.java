package io.notifyhub.spring;

import io.notifyhub.channel.websocket.WebSocketChannel;
import io.notifyhub.channel.websocket.WebSocketConfig;
import io.notifyhub.spring.properties.WebSocketProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket auto-configuration — isolated so classes are only loaded
 * when notify-websocket module is on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "io.notifyhub.channel.websocket.WebSocketChannel")
public class NotifyWebSocketAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotifyWebSocketAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "notify.channels.websocket", name = "uri")
    @ConditionalOnMissingBean(WebSocketChannel.class)
    public WebSocketChannel webSocketChannel(NotifyProperties properties) {
        WebSocketProperties ws = properties.getChannels().getWebsocket();
        WebSocketConfig.Builder builder = WebSocketConfig.builder()
                .uri(ws.getUri())
                .timeoutMs(ws.getTimeoutMs())
                .reconnectEnabled(ws.isReconnectEnabled())
                .reconnectDelayMs(ws.getReconnectDelayMs())
                .maxReconnectAttempts(ws.getMaxReconnectAttempts())
                .messageFormat(ws.getMessageFormat());

        if (ws.getHeaders() != null) {
            builder.headers(ws.getHeaders());
        }

        log.info("NotifyHub: WebSocket channel configured ({})", ws.getUri());
        return new WebSocketChannel(builder.build());
    }
}
