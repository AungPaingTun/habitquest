package com.habitquest.checkin.dto;

import com.habitquest.habit.Frequency;
import com.habitquest.habit.HabitStatus;

import java.time.LocalDate;

/**
 * One habit as shown on the Today page.
 *
 * @param periodCount check-ins so far this week/month (or today, for daily habits)
 * @param canCheckIn  true when the check-in button should be enabled
 */
public record TodayHabit(
        Long id,
        String name,
        String icon,
        int points,
        Frequency frequency,
        int targetCount,
        LocalDate startDate,
        LocalDate endDate,
        HabitStatus status,
        boolean doneToday,
        int periodCount,
        int currentStreak,
        int bestStreak,
        boolean canCheckIn
) {
}
