package io.notifyhub.core.routing;

import io.notifyhub.core.Notifiable;
import io.notifyhub.core.Priority;

import java.util.Collections;
import java.util.Map;

/**
 * Context object passed to {@link RoutingRule} for conditional channel selection.
 * Contains all relevant information about the notification being routed.
 *
 * <pre>{@code
 * RoutingContext ctx = new RoutingContext(user, "user@test.com", params, Priority.NORMAL);
 * Channel channel = rule.evaluate(ctx);
 * }</pre>
 */
public class RoutingContext {

    private final Notifiable notifiable;
    private final String recipient;
    private final Map<String, Object> params;
    private final Priority priority;

    /**
     * Create a new routing context.
     *
     * @param notifiable the notifiable entity (may be null for direct recipients)
     * @param recipient  the resolved recipient address
     * @param params     template parameters / notification metadata
     * @param priority   the notification priority
     */
    public RoutingContext(Notifiable notifiable, String recipient,
                          Map<String, Object> params, Priority priority) {
        this.notifiable = notifiable;
        this.recipient = recipient;
        this.params = params != null ? Collections.unmodifiableMap(params) : Collections.emptyMap();
        this.priority = priority != null ? priority : Priority.NORMAL;
    }

    /**
     * The notifiable entity, if the notification was sent to a Notifiable.
     * May be null when sending to a raw address (email/phone).
     */
    public Notifiable getNotifiable() {
        return notifiable;
    }

    /** The resolved recipient address. */
    public String getRecipient() {
        return recipient;
    }

    /** Template parameters and notification metadata. */
    public Map<String, Object> getParams() {
        return params;
    }

    /** The notification priority. */
    public Priority getPriority() {
        return priority;
    }
}
