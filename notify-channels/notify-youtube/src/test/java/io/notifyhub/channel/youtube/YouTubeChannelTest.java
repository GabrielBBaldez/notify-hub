package io.notifyhub.channel.youtube;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YouTubeChannelTest {

    @Test
    void getName_returnsYoutube() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("test-token")
                .build();
        YouTubeChannel channel = new YouTubeChannel(config);
        assertEquals("youtube", channel.getName());
    }

    @Test
    void isAvailable_returnsTrueWhenConfigured() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("test-token")
                .build();
        YouTubeChannel channel = new YouTubeChannel(config);
        assertTrue(channel.isAvailable());
    }

    @Test
    void config_requiresAccessToken() {
        assertThrows(IllegalArgumentException.class, () ->
                YouTubeConfig.builder().build());
    }

    @Test
    void config_acceptsOptionalFields() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("test-token")
                .channelId("UC123")
                .liveChatId("chat123")
                .timeoutMs(5000)
                .build();
        assertEquals("test-token", config.getAccessToken());
        assertEquals("UC123", config.getChannelId());
        assertEquals("chat123", config.getLiveChatId());
        assertEquals(5000, config.getTimeoutMs());
    }

    @Test
    void send_failsWithNetworkError() {
        YouTubeConfig config = YouTubeConfig.builder()
                .accessToken("test-token")
                .liveChatId("chat123")
                .timeoutMs(100)
                .build();
        YouTubeChannel channel = new YouTubeChannel(config);

        io.notifyhub.core.Notification notification = new io.notifyhub.core.Notification(
                null, "default", null, null, "Hello YouTube!", java.util.Map.of());

        assertThrows(io.notifyhub.core.channel.NotificationSendException.class,
                () -> channel.send(notification));
    }
}
