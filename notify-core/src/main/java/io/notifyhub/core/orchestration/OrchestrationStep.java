package io.notifyhub.core.orchestration;

import java.time.Duration;
import java.util.Map;

public record OrchestrationStep(
    String channelName,
    String templateName,
    String rawContent,
    String subject,
    Map<String, Object> params,
    Duration escalateAfter  // null for the last step (no escalation)
) {
    public OrchestrationStep {
        if (channelName == null) throw new IllegalArgumentException("channelName must not be null");
    }
}
