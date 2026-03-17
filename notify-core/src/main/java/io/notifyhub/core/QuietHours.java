package io.notifyhub.core;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Quiet hours configuration. During quiet hours, notifications are queued
 * and delivered at the next allowed time.
 *
 * <pre>{@code
 * // No quiet hours (always allow)
 * QuietHours.none()
 *
 * // Quiet from 10 PM to 8 AM in São Paulo timezone
 * QuietHours.between(LocalTime.of(22, 0), LocalTime.of(8, 0),
 *     ZoneId.of("America/Sao_Paulo"))
 * }</pre>
 */
public class QuietHours {

    private static final QuietHours NONE = new QuietHours(null, null, null);

    private final LocalTime start;
    private final LocalTime end;
    private final ZoneId timezone;

    private QuietHours(LocalTime start, LocalTime end, ZoneId timezone) {
        this.start = start;
        this.end = end;
        this.timezone = timezone;
    }

    public static QuietHours none() {
        return NONE;
    }

    public static QuietHours between(LocalTime start, LocalTime end, ZoneId timezone) {
        if (start == null || end == null || timezone == null) {
            throw new IllegalArgumentException("start, end, and timezone must not be null");
        }
        return new QuietHours(start, end, timezone);
    }

    /**
     * Returns the next Instant when delivery is allowed.
     * If not currently in quiet period, returns the given instant unchanged.
     * If in quiet period, returns the next allowed time (end of quiet period).
     */
    public Instant nextAllowedTime(Instant now) {
        if (this == NONE || start == null) {
            return now;
        }

        ZonedDateTime zdt = now.atZone(timezone);
        LocalTime currentTime = zdt.toLocalTime();

        boolean inQuietPeriod;
        if (start.isAfter(end)) {
            // Overnight quiet hours (e.g., 22:00 to 08:00)
            inQuietPeriod = !currentTime.isBefore(start) || currentTime.isBefore(end);
        } else {
            // Same-day quiet hours (e.g., 12:00 to 14:00)
            inQuietPeriod = !currentTime.isBefore(start) && currentTime.isBefore(end);
        }

        if (!inQuietPeriod) {
            return now;
        }

        // Calculate next allowed time
        ZonedDateTime nextAllowed;
        if (start.isAfter(end)) {
            // Overnight: next allowed is end time on next day if before midnight,
            // or end time today if after midnight
            if (currentTime.isBefore(end)) {
                nextAllowed = zdt.with(end);
            } else {
                nextAllowed = zdt.plusDays(1).with(end);
            }
        } else {
            nextAllowed = zdt.with(end);
        }

        return nextAllowed.toInstant();
    }

    public boolean isNone() {
        return this == NONE;
    }

    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }
    public ZoneId getTimezone() { return timezone; }
}
