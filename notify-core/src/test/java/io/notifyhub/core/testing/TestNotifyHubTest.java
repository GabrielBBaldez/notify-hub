package io.notifyhub.core.testing;

import io.notifyhub.core.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestNotifyHubTest {

    @Test
    @DisplayName("Should capture sent notifications for assertions")
    void captureSentNotifications() {
        TestNotifyHub testHub = TestNotifyHub.create();
        testHub.to("user@test.com").via(Channel.EMAIL).content("Hello").send();

        assertEquals(1, testHub.sent().size());
        assertEquals("email", testHub.sent().get(0).channel());
        assertEquals("user@test.com", testHub.sent().get(0).recipient());
        assertEquals("Hello", testHub.sent().get(0).content());
    }

    @Test
    @DisplayName("Should capture multiple sends")
    void captureMultipleSends() {
        TestNotifyHub testHub = TestNotifyHub.create();
        testHub.to("a@test.com").via(Channel.EMAIL).content("First").send();
        testHub.to("b@test.com").via(Channel.EMAIL).content("Second").send();

        assertEquals(2, testHub.sent().size());
    }

    @Test
    @DisplayName("Should filter by channel name")
    void filterByChannel() {
        TestNotifyHub testHub = TestNotifyHub.create();
        testHub.to("user@test.com").via(Channel.EMAIL).content("Email").send();
        testHub.to("user@test.com").via(Channel.SLACK).content("Slack").send();

        assertEquals(1, testHub.sent("email").size());
        assertEquals(1, testHub.sent("slack").size());
    }

    @Test
    @DisplayName("Should reset captured notifications")
    void resetCaptured() {
        TestNotifyHub testHub = TestNotifyHub.create();
        testHub.to("user@test.com").via(Channel.EMAIL).content("Hello").send();
        assertEquals(1, testHub.sent().size());

        testHub.reset();
        assertEquals(0, testHub.sent().size());
    }

    @Test
    @DisplayName("Should expose underlying hub via hub()")
    void exposeUnderlyingHub() {
        TestNotifyHub testHub = TestNotifyHub.create();
        assertNotNull(testHub.hub());
    }

    @Test
    @DisplayName("Should register mock channels for all built-in Channel enum values")
    void registerAllChannels() {
        TestNotifyHub testHub = TestNotifyHub.create();
        for (Channel ch : Channel.values()) {
            String name = ch.name().toLowerCase().replace('_', '-');
            assertTrue(testHub.hub().getRegisteredChannels().contains(name),
                    "Expected channel '" + name + "' to be registered");
        }
    }

    @Test
    @DisplayName("Should capture subject when set")
    void captureSubject() {
        TestNotifyHub testHub = TestNotifyHub.create();
        testHub.to("user@test.com").via(Channel.EMAIL).subject("Test Subject").content("Body").send();

        assertEquals("Test Subject", testHub.sent().get(0).subject());
    }

    @Test
    @DisplayName("Should return empty list when no notifications sent")
    void emptyWhenNothingSent() {
        TestNotifyHub testHub = TestNotifyHub.create();
        assertTrue(testHub.sent().isEmpty());
        assertTrue(testHub.sent("email").isEmpty());
    }

    @Test
    @DisplayName("Should handle hyphenated channel names correctly")
    void handleHyphenatedChannelNames() {
        TestNotifyHub testHub = TestNotifyHub.create();
        testHub.to("user@test.com").via(Channel.TIKTOK_SHOP).content("TikTok message").send();
        testHub.to("user@test.com").via(Channel.GOOGLE_CHAT).content("Google Chat message").send();

        assertEquals(1, testHub.sent("tiktok-shop").size());
        assertEquals(1, testHub.sent("google-chat").size());
    }
}
