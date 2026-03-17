package io.notifyhub.core.schedule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class CronExpressionTest {

    @Test
    @DisplayName("Should compute next fire time for weekday mornings")
    void weekdayMornings() {
        CronExpression cron = CronExpression.parse("0 9 * * MON-FRI");
        // Given Monday 10:00, next should be Tuesday 09:00
        Instant monday10 = ZonedDateTime.of(2026, 3, 16, 10, 0, 0, 0,
            ZoneId.of("UTC")).toInstant();
        Instant tuesday9 = ZonedDateTime.of(2026, 3, 17, 9, 0, 0, 0,
            ZoneId.of("UTC")).toInstant();
        assertEquals(tuesday9, cron.nextFireTime(monday10, ZoneId.of("UTC")));
    }

    @Test
    @DisplayName("Should compute next fire time for every 15 minutes")
    void every15Minutes() {
        CronExpression cron = CronExpression.parse("*/15 * * * *");
        Instant at10_05 = ZonedDateTime.of(2026, 3, 17, 10, 5, 0, 0,
            ZoneId.of("UTC")).toInstant();
        Instant at10_15 = ZonedDateTime.of(2026, 3, 17, 10, 15, 0, 0,
            ZoneId.of("UTC")).toInstant();
        assertEquals(at10_15, cron.nextFireTime(at10_05, ZoneId.of("UTC")));
    }

    @Test
    @DisplayName("Should compute next fire time for specific day of month")
    void specificDayOfMonth() {
        CronExpression cron = CronExpression.parse("0 0 1 * *"); // midnight on the 1st
        Instant march15 = ZonedDateTime.of(2026, 3, 15, 12, 0, 0, 0,
            ZoneId.of("UTC")).toInstant();
        Instant april1 = ZonedDateTime.of(2026, 4, 1, 0, 0, 0, 0,
            ZoneId.of("UTC")).toInstant();
        assertEquals(april1, cron.nextFireTime(march15, ZoneId.of("UTC")));
    }

    @Test
    @DisplayName("Should reject invalid expression")
    void invalidExpression() {
        assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("0 9 *"));
    }

    @Test
    @DisplayName("Should handle Sunday as both 0 and 7")
    void sundayHandling() {
        CronExpression cron = CronExpression.parse("0 9 * * SUN");
        // Given Monday 2026-03-16, next Sunday is 2026-03-22
        Instant monday = ZonedDateTime.of(2026, 3, 16, 10, 0, 0, 0,
            ZoneId.of("UTC")).toInstant();
        Instant sunday = ZonedDateTime.of(2026, 3, 22, 9, 0, 0, 0,
            ZoneId.of("UTC")).toInstant();
        assertEquals(sunday, cron.nextFireTime(monday, ZoneId.of("UTC")));
    }

    @Test
    @DisplayName("Should handle comma-separated values")
    void commaSeparated() {
        CronExpression cron = CronExpression.parse("0 9,17 * * *"); // 9am and 5pm
        Instant at10 = ZonedDateTime.of(2026, 3, 17, 10, 0, 0, 0,
            ZoneId.of("UTC")).toInstant();
        Instant at17 = ZonedDateTime.of(2026, 3, 17, 17, 0, 0, 0,
            ZoneId.of("UTC")).toInstant();
        assertEquals(at17, cron.nextFireTime(at10, ZoneId.of("UTC")));
    }
}
