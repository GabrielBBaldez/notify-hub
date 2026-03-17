package io.notifyhub.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class QuietHoursTest {

    @Test
    @DisplayName("Should return now when not in quiet period")
    void notInQuietPeriod() {
        QuietHours qh = QuietHours.between(LocalTime.of(22, 0), LocalTime.of(8, 0),
            ZoneId.of("America/Sao_Paulo"));
        Instant noon = ZonedDateTime.of(2026, 3, 17, 12, 0, 0, 0,
            ZoneId.of("America/Sao_Paulo")).toInstant();
        assertEquals(noon, qh.nextAllowedTime(noon));
    }

    @Test
    @DisplayName("Should return next morning when in quiet period (late night)")
    void inQuietPeriodLateNight() {
        QuietHours qh = QuietHours.between(LocalTime.of(22, 0), LocalTime.of(8, 0),
            ZoneId.of("America/Sao_Paulo"));
        Instant lateNight = ZonedDateTime.of(2026, 3, 17, 23, 30, 0, 0,
            ZoneId.of("America/Sao_Paulo")).toInstant();
        Instant nextMorning = ZonedDateTime.of(2026, 3, 18, 8, 0, 0, 0,
            ZoneId.of("America/Sao_Paulo")).toInstant();
        assertEquals(nextMorning, qh.nextAllowedTime(lateNight));
    }

    @Test
    @DisplayName("Should return same day morning when in quiet period (early morning)")
    void inQuietPeriodEarlyMorning() {
        QuietHours qh = QuietHours.between(LocalTime.of(22, 0), LocalTime.of(8, 0),
            ZoneId.of("America/Sao_Paulo"));
        Instant earlyMorning = ZonedDateTime.of(2026, 3, 17, 3, 0, 0, 0,
            ZoneId.of("America/Sao_Paulo")).toInstant();
        Instant morning = ZonedDateTime.of(2026, 3, 17, 8, 0, 0, 0,
            ZoneId.of("America/Sao_Paulo")).toInstant();
        assertEquals(morning, qh.nextAllowedTime(earlyMorning));
    }

    @Test
    @DisplayName("QuietHours.none() always returns now")
    void noneAlwaysReturnsNow() {
        Instant now = Instant.now();
        assertEquals(now, QuietHours.none().nextAllowedTime(now));
    }

    @Test
    @DisplayName("Should handle same-day quiet hours (e.g., lunch)")
    void sameDayQuietHours() {
        QuietHours qh = QuietHours.between(LocalTime.of(12, 0), LocalTime.of(14, 0),
            ZoneId.of("UTC"));
        Instant lunch = ZonedDateTime.of(2026, 3, 17, 13, 0, 0, 0,
            ZoneId.of("UTC")).toInstant();
        Instant afterLunch = ZonedDateTime.of(2026, 3, 17, 14, 0, 0, 0,
            ZoneId.of("UTC")).toInstant();
        assertEquals(afterLunch, qh.nextAllowedTime(lunch));
    }
}
