package com.habitquest.habit.dto;

import com.habitquest.habit.Frequency;
import com.habitquest.habit.Habit;

import java.time.Instant;
import java.time.LocalDate;

public record HabitResponse(
        Long id,
        String name,
        String icon,
        int points,
        Frequency frequency,
        int targetCount,
        LocalDate startDate,
        LocalDate endDate,
        boolean archived,
        // true once the habit has a check-in: frequency and target can no longer change (PRD §5.1)
        boolean rulesLocked,
        Instant createdAt
) {
    public static HabitResponse from(Habit habit, boolean rulesLocked) {
        return new HabitResponse(habit.getId(), habit.getName(), habit.getIcon(), habit.getPoints(),
                habit.getFrequency(), habit.getTargetCount(), habit.getStartDate(), habit.getEndDate(),
                habit.isArchived(), rulesLocked, habit.getCreatedAt());
    }
}
