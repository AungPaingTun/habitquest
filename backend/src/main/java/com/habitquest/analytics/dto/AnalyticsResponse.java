package com.habitquest.analytics.dto;

import com.habitquest.analytics.Period;
import com.habitquest.habit.Frequency;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the Analytics page shows for one week, month or year.
 *
 * @param start  first day of the period
 * @param end    last day of the period (may be after today for the current period)
 * @param today  today in the user's time zone; days after it are marked {@code future} in the series
 * @param series one bucket per day (week/month) or per month (year)
 */
public record AnalyticsResponse(
        Period period,
        LocalDate start,
        LocalDate end,
        LocalDate today,
        Summary summary,
        List<HabitStats> habits,
        List<Bucket> series,
        List<PrizeStat> topPrizes
) {

    /**
     * @param completionRate 0–100: check-ins done vs expected so far (null if nothing was expected)
     * @param pointsEarned   check-ins + streak bonuses − undos
     * @param pointsSpent    points spent on prizes (positive number)
     * @param activeDays     days with at least one check-in
     */
    public record Summary(int checkIns, Integer completionRate, long pointsEarned, long pointsSpent,
                          int redemptions, int activeDays) {
    }

    /**
     * @param expected       check-ins the habit's target asks for in the elapsed part of the period
     *                       (e.g. 2×/week over 3½ days = 1.0)
     * @param completionRate 0–100, capped at 100; null if nothing was expected yet
     */
    public record HabitStats(Long id, String name, String icon, Frequency frequency, int targetCount,
                             boolean archived, int checkIns, double expected, Integer completionRate,
                             int currentStreak, int bestStreak) {
    }

    /** @param key "2026-10-02" for a day bucket, "2026-10" for a month bucket */
    public record Bucket(String key, long earned, long spent, int checkIns, boolean future) {
    }

    public record PrizeStat(Long id, String name, String icon, int count, long pointsSpent) {
    }
}
