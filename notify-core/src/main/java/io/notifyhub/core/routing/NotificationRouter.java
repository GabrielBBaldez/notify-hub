package io.notifyhub.core.routing;

import io.notifyhub.core.Channel;

import java.util.List;

/**
 * Routes notifications to channels by evaluating a chain of {@link RoutingRule}s.
 * The first rule that returns a non-null channel wins.
 *
 * <pre>{@code
 * NotificationRouter router = new NotificationRouter(
 *     RoutingRule.timeBased(LocalTime.of(9, 0), LocalTime.of(18, 0),
 *         Channel.SMS, Channel.EMAIL),
 *     RoutingRule.propertyBased("urgency",
 *         Map.of("high", Channel.SMS),
 *         Channel.EMAIL)
 * );
 *
 * NotifyHub hub = NotifyHub.builder()
 *     .router(router)
 *     .build();
 * }</pre>
 */
public class NotificationRouter {

    private final List<RoutingRule> rules;

    /**
     * Create a router with a list of rules evaluated in order.
     *
     * @param rules the routing rules to evaluate
     */
    public NotificationRouter(List<RoutingRule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Create a router with varargs rules evaluated in order.
     *
     * @param rules the routing rules to evaluate
     */
    public NotificationRouter(RoutingRule... rules) {
        this.rules = List.of(rules);
    }

    /**
     * Route a notification by evaluating each rule in order.
     * Returns the first non-null channel, or {@code null} if no rule matched.
     *
     * @param context the routing context
     * @return the selected channel, or null if no rule matched
     */
    public Channel route(RoutingContext context) {
        for (RoutingRule rule : rules) {
            Channel ch = rule.evaluate(context);
            if (ch != null) {
                return ch;
            }
        }
        return null;
    }
}
