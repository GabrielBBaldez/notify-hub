package io.notifyhub.core;

import java.util.Objects;

/**
 * Reference to a custom notification channel by name.
 * Created via {@link Channel#custom(String)}.
 */
public final class ChannelRef {

    private final String name;

    ChannelRef(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Channel name must not be null or blank");
        }
        this.name = name.toLowerCase().trim();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelRef that = (ChannelRef) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "ChannelRef{" + name + "}";
    }
}
