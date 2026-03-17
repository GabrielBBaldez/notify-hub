package io.notifyhub.core.orchestration;

import io.notifyhub.core.Channel;

import java.time.Duration;
import java.util.*;

/**
 * Builder for multi-channel notification orchestration.
 *
 * <pre>{@code
 * notify.to(user).orchestrate()
 *     .first(Channel.EMAIL).template("promo")
 *     .ifNoOpen(Duration.ofHours(24))
 *     .then(Channel.PUSH).content("Check your email!")
 *     .execute();
 * }</pre>
 */
public class OrchestrationBuilder {

    private final String recipientEmail;
    private final String recipientPhone;
    private final String recipientPushToken;
    private final List<StepBuilder> stepBuilders = new ArrayList<>();
    private StepBuilder currentStep;

    public OrchestrationBuilder(String recipientEmail, String recipientPhone, String recipientPushToken) {
        this.recipientEmail = recipientEmail;
        this.recipientPhone = recipientPhone;
        this.recipientPushToken = recipientPushToken;
    }

    public OrchestrationBuilder first(Channel channel) {
        currentStep = new StepBuilder(channelToName(channel));
        stepBuilders.add(currentStep);
        return this;
    }

    public OrchestrationBuilder template(String templateName) {
        currentStep.templateName = templateName;
        return this;
    }

    public OrchestrationBuilder content(String content) {
        currentStep.rawContent = content;
        return this;
    }

    public OrchestrationBuilder subject(String subject) {
        currentStep.subject = subject;
        return this;
    }

    public OrchestrationBuilder param(String key, Object value) {
        currentStep.params.put(key, value);
        return this;
    }

    public OrchestrationBuilder ifNoOpen(Duration timeout) {
        currentStep.escalateAfter = timeout;
        return this;
    }

    public OrchestrationBuilder then(Channel channel) {
        currentStep = new StepBuilder(channelToName(channel));
        stepBuilders.add(currentStep);
        return this;
    }

    public List<OrchestrationStep> getSteps() {
        return stepBuilders.stream()
            .map(StepBuilder::build)
            .toList();
    }

    private static String channelToName(Channel ch) {
        return ch.name().toLowerCase().replace('_', '-');
    }

    private static class StepBuilder {
        String channelName;
        String templateName;
        String rawContent;
        String subject;
        Map<String, Object> params = new LinkedHashMap<>();
        Duration escalateAfter;

        StepBuilder(String channelName) {
            this.channelName = channelName;
        }

        OrchestrationStep build() {
            return new OrchestrationStep(
                channelName, templateName, rawContent, subject,
                params.isEmpty() ? Map.of() : Map.copyOf(params),
                escalateAfter
            );
        }
    }
}
