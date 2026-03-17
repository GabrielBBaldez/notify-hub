package io.notifyhub.core.schedule;

import java.time.*;
import java.util.*;

/**
 * Lightweight cron expression parser supporting standard 5-field format.
 *
 * <p>Fields: minute hour day-of-month month day-of-week</p>
 *
 * <p>Supported syntax:</p>
 * <ul>
 *   <li>{@code *} — any value</li>
 *   <li>{@code 5} — exact value</li>
 *   <li>{@code 1-5} — range</li>
 *   <li>{@code 1,3,5} — list</li>
 *   <li>{@code * /15} — step (every 15)</li>
 *   <li>{@code MON-FRI} — day-of-week names</li>
 *   <li>{@code JAN-DEC} — month names</li>
 * </ul>
 *
 * <pre>{@code
 * CronExpression cron = CronExpression.parse("0 9 * * MON-FRI");
 * Instant next = cron.nextFireTime(Instant.now(), ZoneId.systemDefault());
 * }</pre>
 */
public class CronExpression {

    private final String expression;
    private final Set<Integer> minutes;
    private final Set<Integer> hours;
    private final Set<Integer> daysOfMonth;
    private final Set<Integer> months;
    private final Set<Integer> daysOfWeek; // 1=Monday, 7=Sunday (ISO)

    private CronExpression(String expression, Set<Integer> minutes, Set<Integer> hours,
                           Set<Integer> daysOfMonth, Set<Integer> months, Set<Integer> daysOfWeek) {
        this.expression = expression;
        this.minutes = minutes;
        this.hours = hours;
        this.daysOfMonth = daysOfMonth;
        this.months = months;
        this.daysOfWeek = daysOfWeek;
    }

    public static CronExpression parse(String expression) {
        // Parse 5-field cron: minute hour dayOfMonth month dayOfWeek
        String[] fields = expression.trim().split("\\s+");
        if (fields.length != 5) {
            throw new IllegalArgumentException(
                "Cron expression must have 5 fields (minute hour day-of-month month day-of-week), got: " + fields.length);
        }

        Set<Integer> minutes = parseField(fields[0], 0, 59);
        Set<Integer> hours = parseField(fields[1], 0, 23);
        Set<Integer> daysOfMonth = parseField(fields[2], 1, 31);
        Set<Integer> months = parseMonthField(fields[3]);
        Set<Integer> daysOfWeek = parseDayOfWeekField(fields[4]);

        return new CronExpression(expression, minutes, hours, daysOfMonth, months, daysOfWeek);
    }

    /**
     * Compute the next fire time after the given instant.
     */
    public Instant nextFireTime(Instant after, ZoneId zone) {
        ZonedDateTime zdt = after.atZone(zone).plusMinutes(1).withSecond(0).withNano(0);

        // Search up to 1 year ahead
        ZonedDateTime limit = zdt.plusYears(1);

        while (zdt.isBefore(limit)) {
            if (!months.contains(zdt.getMonthValue())) {
                zdt = zdt.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0);
                continue;
            }
            if (!daysOfMonth.contains(zdt.getDayOfMonth()) || !daysOfWeek.contains(zdt.getDayOfWeek().getValue())) {
                zdt = zdt.plusDays(1).withHour(0).withMinute(0);
                continue;
            }
            if (!hours.contains(zdt.getHour())) {
                zdt = zdt.plusHours(1).withMinute(0);
                continue;
            }
            if (!minutes.contains(zdt.getMinute())) {
                zdt = zdt.plusMinutes(1);
                continue;
            }
            return zdt.toInstant();
        }

        throw new IllegalStateException("No next fire time found within 1 year for: " + expression);
    }

    public String getExpression() { return expression; }

    // ===================== PARSING =====================

    private static Set<Integer> parseField(String field, int min, int max) {
        Set<Integer> values = new TreeSet<>();
        for (String part : field.split(",")) {
            if (part.contains("/")) {
                String[] stepParts = part.split("/");
                int step = Integer.parseInt(stepParts[1]);
                int start = stepParts[0].equals("*") ? min : Integer.parseInt(stepParts[0]);
                for (int i = start; i <= max; i += step) {
                    values.add(i);
                }
            } else if (part.equals("*")) {
                for (int i = min; i <= max; i++) values.add(i);
            } else if (part.contains("-")) {
                String[] range = part.split("-");
                int from = Integer.parseInt(range[0]);
                int to = Integer.parseInt(range[1]);
                for (int i = from; i <= to; i++) values.add(i);
            } else {
                values.add(Integer.parseInt(part));
            }
        }
        return Collections.unmodifiableSet(values);
    }

    private static Set<Integer> parseMonthField(String field) {
        field = field.toUpperCase()
            .replace("JAN", "1").replace("FEB", "2").replace("MAR", "3")
            .replace("APR", "4").replace("MAY", "5").replace("JUN", "6")
            .replace("JUL", "7").replace("AUG", "8").replace("SEP", "9")
            .replace("OCT", "10").replace("NOV", "11").replace("DEC", "12");
        return parseField(field, 1, 12);
    }

    private static Set<Integer> parseDayOfWeekField(String field) {
        // Convert to ISO: 1=Monday, 7=Sunday
        field = field.toUpperCase()
            .replace("MON", "1").replace("TUE", "2").replace("WED", "3")
            .replace("THU", "4").replace("FRI", "5").replace("SAT", "6").replace("SUN", "7");
        // Also handle numeric 0=Sunday convention
        Set<Integer> parsed = parseField(field, 0, 7);
        Set<Integer> iso = new TreeSet<>();
        for (int v : parsed) {
            if (v == 0) iso.add(7); // 0 = Sunday -> 7 in ISO
            else iso.add(v);
        }
        return Collections.unmodifiableSet(iso);
    }
}
