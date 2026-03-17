package io.notifyhub.core.orchestration;

import io.notifyhub.core.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrchestrationBuilderTest {

    @Test
    @DisplayName("Should build orchestration with email then push fallback")
    void buildOrchestration() {
        OrchestrationBuilder builder = new OrchestrationBuilder("user@test.com", null, "push-token");
        builder.first(Channel.EMAIL).template("promo")
               .ifNoOpen(Duration.ofHours(24))
               .then(Channel.PUSH).content("Check your email!");

        List<OrchestrationStep> steps = builder.getSteps();
        assertEquals(2, steps.size());
        assertEquals("email", steps.get(0).channelName());
        assertEquals("promo", steps.get(0).templateName());
        assertEquals(Duration.ofHours(24), steps.get(0).escalateAfter());
        assertEquals("push", steps.get(1).channelName());
        assertEquals("Check your email!", steps.get(1).rawContent());
        assertNull(steps.get(1).escalateAfter());
    }

    @Test
    @DisplayName("Should build single-step orchestration")
    void singleStep() {
        OrchestrationBuilder builder = new OrchestrationBuilder("user@test.com", null, null);
        builder.first(Channel.EMAIL).content("Hello");

        assertEquals(1, builder.getSteps().size());
    }

    @Test
    @DisplayName("Should build multi-step orchestration with three channels")
    void threeSteps() {
        OrchestrationBuilder builder = new OrchestrationBuilder("user@test.com", "+5511999", "token");
        builder.first(Channel.EMAIL).template("welcome")
               .ifNoOpen(Duration.ofHours(12))
               .then(Channel.SMS).content("Check email!")
               .ifNoOpen(Duration.ofHours(6))
               .then(Channel.PUSH).content("Final reminder");

        List<OrchestrationStep> steps = builder.getSteps();
        assertEquals(3, steps.size());
    }

    @Test
    @DisplayName("Should support template params per step")
    void paramsPerStep() {
        OrchestrationBuilder builder = new OrchestrationBuilder("user@test.com", null, null);
        builder.first(Channel.EMAIL)
               .template("promo").param("code", "SAVE20").param("name", "John");

        OrchestrationStep step = builder.getSteps().get(0);
        assertEquals("SAVE20", step.params().get("code"));
        assertEquals("John", step.params().get("name"));
    }
}
