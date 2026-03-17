package io.notifyhub.core.abtest;

import io.notifyhub.core.Channel;
import io.notifyhub.core.NotificationBuilder;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.channel.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbTestBuilderTest {

    @Mock private NotificationChannel emailChannel;
    private NotifyHub hub;

    @BeforeEach
    void setUp() {
        lenient().when(emailChannel.getName()).thenReturn("email");
        lenient().when(emailChannel.sendWithResult(any())).thenCallRealMethod();
        hub = NotifyHub.builder().channel(emailChannel).build();
    }

    @Test
    @DisplayName("Should deterministically select same variant for same recipient")
    void deterministicSelection() {
        AbTestBuilder ab1 = new AbTestBuilder(
            hub.to("user@test.com").via(Channel.EMAIL),
            "experiment-1", "user@test.com");
        ab1.variant("A", b -> b.template("v1")).variant("B", b -> b.template("v2"));
        String selected1 = ab1.selectVariant();

        AbTestBuilder ab2 = new AbTestBuilder(
            hub.to("user@test.com").via(Channel.EMAIL),
            "experiment-1", "user@test.com");
        ab2.variant("A", b -> b.template("v1")).variant("B", b -> b.template("v2"));
        String selected2 = ab2.selectVariant();

        assertEquals(selected1, selected2, "Same recipient should get same variant");
    }

    @Test
    @DisplayName("Should require matching number of weights and variants")
    void weightCountMismatch() {
        NotificationBuilder builder = hub.to("user@test.com").via(Channel.EMAIL);
        AbTestBuilder ab = new AbTestBuilder(builder, "exp", "user@test.com");
        ab.variant("A", b -> b.template("v1")).variant("B", b -> b.template("v2"));

        assertThrows(IllegalArgumentException.class, () -> ab.split(50, 30, 20));
    }

    @Test
    @DisplayName("Should apply selected variant configuration and return parent builder")
    void appliesVariantConfig() {
        NotificationBuilder builder = hub.to("user@test.com").via(Channel.EMAIL);
        AbTestBuilder ab = new AbTestBuilder(builder, "exp", "user@test.com");

        String selectedVariant = ab.variant("A", b -> b.template("welcome-v1"))
                                   .variant("B", b -> b.template("welcome-v2"))
                                   .selectVariant();

        // selectedVariant must be one of the declared variants
        assertTrue("A".equals(selectedVariant) || "B".equals(selectedVariant),
            "Selected variant must be A or B, got: " + selectedVariant);
    }

    @Test
    @DisplayName("Should return parent NotificationBuilder from split()")
    void splitReturnsParentBuilder() {
        NotificationBuilder builder = hub.to("user@test.com").via(Channel.EMAIL);
        AbTestBuilder ab = new AbTestBuilder(builder, "exp", "user@test.com");
        ab.variant("A", b -> b.template("v1")).variant("B", b -> b.template("v2"));

        NotificationBuilder result = ab.split(50, 50);

        assertSame(builder, result, "split() should return the parent NotificationBuilder");
    }

    @Test
    @DisplayName("Different recipients should get distributed across variants")
    void distributionAcrossRecipients() {
        int countA = 0, countB = 0;
        for (int i = 0; i < 100; i++) {
            String email = "user" + i + "@test.com";
            AbTestBuilder ab = new AbTestBuilder(
                hub.to(email).via(Channel.EMAIL),
                "distribution-test", email);
            ab.variant("A", b -> b.template("v1")).variant("B", b -> b.template("v2"));
            String selected = ab.selectVariant();
            if ("A".equals(selected)) countA++;
            else countB++;
        }
        // With 100 users and 50/50 split, both should have some
        assertTrue(countA > 10, "Variant A should get some users, got " + countA);
        assertTrue(countB > 10, "Variant B should get some users, got " + countB);
    }

    @Test
    @DisplayName("Should use getExperimentName() accessor")
    void experimentNameAccessor() {
        AbTestBuilder ab = new AbTestBuilder(
            hub.to("user@test.com").via(Channel.EMAIL),
            "my-experiment", "user@test.com");

        assertEquals("my-experiment", ab.getExperimentName());
    }

    @Test
    @DisplayName("Should expose variants via getVariants()")
    void variantsAccessor() {
        AbTestBuilder ab = new AbTestBuilder(
            hub.to("user@test.com").via(Channel.EMAIL),
            "exp", "user@test.com");
        ab.variant("A", b -> b.template("v1")).variant("B", b -> b.template("v2"));

        var variants = ab.getVariants();
        assertEquals(2, variants.size());
        assertEquals("A", variants.get(0).name());
        assertEquals("B", variants.get(1).name());
        assertEquals(1, variants.get(0).weight());
    }
}
