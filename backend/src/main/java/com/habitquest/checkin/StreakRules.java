package com.habitquest.checkin;

import com.habitquest.habit.Frequency;

/**
 * Streak bonus rules (PRD §5.4): flat extra points, never multipliers.
 * All thresholds and amounts live here so they're easy to tune.
 */
public final class StreakRules {

    // Daily habits: streak counted in days.
    static final int DAILY_TIER_1_DAYS = 7;
    static final int DAILY_TIER_2_DAYS = 30;

    // Weekly/monthly habits: streak counted in periods where the target was met.
    static final int PERIOD_TIER_1 = 3;
    static final int PERIOD_TIER_2 = 8;

    static final int TIER_1_BONUS = 2;
    static final int TIER_2_BONUS = 4;

    private StreakRules() {
    }

    /** Bonus for a check-in that extends a streak to {@code streak} days (daily) or periods (weekly/monthly). */
    public static int bonusFor(Frequency frequency, int streak) {
        int tier1 = frequency == Frequency.DAILY ? DAILY_TIER_1_DAYS : PERIOD_TIER_1;
        int tier2 = frequency == Frequency.DAILY ? DAILY_TIER_2_DAYS : PERIOD_TIER_2;
        if (streak >= tier2) return TIER_2_BONUS;
        if (streak >= tier1) return TIER_1_BONUS;
        return 0;
    }
}
