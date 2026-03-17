package io.notifyhub.core.abtest;

import java.util.function.Consumer;
import io.notifyhub.core.NotificationBuilder;

public record AbTestVariant(
    String name,
    Consumer<NotificationBuilder> configurator,
    int weight
) {
    public AbTestVariant {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        if (configurator == null) throw new IllegalArgumentException("configurator must not be null");
        if (weight <= 0) throw new IllegalArgumentException("weight must be positive");
    }
}
