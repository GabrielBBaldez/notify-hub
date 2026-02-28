package io.notifyhub.core.audience;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A named audience defined by a set of tag filters (AND logic).
 * Contacts must have ALL listed tags to be included in the audience.
 *
 * <pre>{@code
 * Audience vips = new Audience("premium-users", Set.of("vip", "plan:premium"));
 * // Matches contacts that have BOTH "vip" AND "plan:premium" tags
 * }</pre>
 */
public class Audience {

    private final String name;
    private final Set<String> tags;

    public Audience(String name, Set<String> tags) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Audience name must not be blank");
        }
        this.name = name;
        this.tags = tags != null ? new LinkedHashSet<>(tags) : new LinkedHashSet<>();
    }

    public String getName() { return name; }

    public Set<String> getTags() { return Collections.unmodifiableSet(tags); }

    @Override
    public String toString() {
        return "Audience{name='" + name + "', tags=" + tags + "}";
    }
}
