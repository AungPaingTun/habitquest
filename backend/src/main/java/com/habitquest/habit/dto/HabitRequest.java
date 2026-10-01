package com.habitquest.habit.dto;

import com.habitquest.habit.Frequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Body for creating or editing a habit. Cross-field rules (target vs frequency, dates) live in HabitService. */
public record HabitRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 16) String icon,
        @NotNull @Min(1) @Max(100) Integer points,
        // Optional: defaults to DAILY.
        Frequency frequency,
        // Optional: defaults to 1. Times per week/month for WEEKLY/MONTHLY habits.
        @Min(1) Integer targetCount,
        // Optional: defaults to today in the user's time zone.
        LocalDate startDate,
        // Optional: null means no end date.
        LocalDate endDate
) {
    public HabitRequest {
        name = name == null ? null : name.trim();
        icon = icon == null || icon.isBlank() ? null : icon.trim();
    }
}
