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
        Instant createdAt
) {
    public static HabitResponse from(Habit habit) {
        return new HabitResponse(habit.getId(), habit.getName(), habit.getIcon(), habit.getPoints(),
                habit.getFrequency(), habit.getTargetCount(), habit.getStartDate(), habit.getEndDate(),
                habit.isArchived(), habit.getCreatedAt());
    }
}
