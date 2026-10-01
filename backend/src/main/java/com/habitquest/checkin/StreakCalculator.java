package com.habitquest.checkin;

import com.habitquest.habit.Frequency;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure streak maths, no database, so it's easy to unit test.
 *
 * A "period" is a day (DAILY), a Monday–Sunday week (WEEKLY) or a calendar month (MONTHLY).
 * A period counts toward the streak when it has at least {@code target} check-ins.
 * The streak is the number of such periods in a row, ending now.
 */
public final class StreakCalculator {

    public record Streak(int current, int best) {
    }

    private StreakCalculator() {
    }

    /** The first day of the period containing {@code date}. */
    public static LocalDate periodStart(Frequency frequency, LocalDate date) {
        return switch (frequency) {
            case DAILY -> date;
            case WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> date.withDayOfMonth(1);
        };
    }

    static LocalDate previousPeriodStart(Frequency frequency, LocalDate periodStart) {
        return switch (frequency) {
            case DAILY -> periodStart.minusDays(1);
            case WEEKLY -> periodStart.minusWeeks(1);
            case MONTHLY -> periodStart.minusMonths(1);
        };
    }

    /**
     * @param logDates days the habit was checked in (any order, no duplicates)
     * @param today    today in the user's time zone
     */
    public static Streak calculate(Frequency frequency, int target, Collection<LocalDate> logDates, LocalDate today) {
        Map<LocalDate, Long> checkInsPerPeriod = logDates.stream()
                .collect(Collectors.groupingBy(d -> periodStart(frequency, d), Collectors.counting()));
        Set<LocalDate> metPeriods = checkInsPerPeriod.entrySet().stream()
                .filter(e -> e.getValue() >= target)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(HashSet::new));

        // Current streak: count back from this period. If this period isn't met yet, the streak is still
        // alive (there's time left), so start counting from the previous period instead.
        LocalDate thisPeriod = periodStart(frequency, today);
        LocalDate cursor = metPeriods.contains(thisPeriod) ? thisPeriod : previousPeriodStart(frequency, thisPeriod);
        int current = 0;
        while (metPeriods.contains(cursor)) {
            current++;
            cursor = previousPeriodStart(frequency, cursor);
        }

        // Best streak: longest run of back-to-back met periods anywhere in history.
        List<LocalDate> sorted = metPeriods.stream().sorted().toList();
        int best = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate period : sorted) {
            run = previous != null && previousPeriodStart(frequency, period).equals(previous) ? run + 1 : 1;
            best = Math.max(best, run);
            previous = period;
        }

        return new Streak(current, Math.max(best, current));
    }

    /** Check-ins in the period containing {@code today}. */
    public static int countInCurrentPeriod(Frequency frequency, Collection<LocalDate> logDates, LocalDate today) {
        LocalDate start = periodStart(frequency, today);
        return (int) logDates.stream().filter(d -> !d.isBefore(start) && !d.isAfter(today)).count();
    }
}
