package io.notifyhub.core.routing;

import io.notifyhub.core.Channel;

import java.time.LocalTime;
import java.util.Map;

/**
 * A functional interface that evaluates a routing context and returns the
 * appropriate channel for the notification.
 *
 * <p>Return {@code null} to indicate that this rule does not apply, allowing
 * the next rule in the chain to be evaluated.</p>
 *
 * <pre>{@code
 * // Time-based routing: SMS during business hours, email after hours
 * RoutingRule rule = RoutingRule.timeBased(
 *     LocalTime.of(9, 0), LocalTime.of(18, 0),
 *     Channel.SMS, Channel.EMAIL);
 *
 * // Property-based routing: route by notification type
 * RoutingRule rule = RoutingRule.propertyBased("type",
 *     Map.of("alert", Channel.SMS, "digest", Channel.EMAIL),
 *     Channel.EMAIL);
 * }</pre>
 */
@FunctionalInterface
public interface RoutingRule {

    /**
     * Evaluate the routing context and return the channel to use.
     *
     * @param context the routing context containing recipient and notification info
     * @return the channel to route to, or {@code null} if this rule doesn't apply
     */
    Channel evaluate(RoutingContext context);

    /**
     * Create a time-based routing rule that selects different channels
     * depending on whether the current time falls within business hours.
     *
     * @param start         start of the business hours window (inclusive)
     * @param end           end of the business hours window (exclusive)
     * @param businessHours channel to use during business hours
     * @param afterHours    channel to use outside business hours
     * @return a routing rule based on time of day
     */
    static RoutingRule timeBased(LocalTime start, LocalTime end,
                                  Channel businessHours, Channel afterHours) {
        return ctx -> {
            LocalTime now = LocalTime.now();
            return (now.isAfter(start) && now.isBefore(end)) ? businessHours : afterHours;
        };
    }

    /**
     * Create a property-based routing rule that selects a channel based on
     * a parameter value in the routing context.
     *
     * @param paramKey       the parameter key to look up in context params
     * @param mappings       map of parameter values to channels
     * @param defaultChannel channel to use if the parameter is missing or unmapped
     * @return a routing rule based on a context parameter
     */
    static RoutingRule propertyBased(String paramKey, Map<String, Channel> mappings,
                                      Channel defaultChannel) {
        return ctx -> {
            Object val = ctx.getParams().get(paramKey);
            return val != null
                    ? mappings.getOrDefault(val.toString(), defaultChannel)
                    : defaultChannel;
        };
    }
}
